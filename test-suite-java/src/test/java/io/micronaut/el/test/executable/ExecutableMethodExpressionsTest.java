package io.micronaut.el.test.executable;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.ELMethod;
import io.micronaut.el.resolver.ELResolvers;
import io.micronaut.el.resolver.ExecutableMethodELExecutor;
import io.micronaut.el.resolver.IntrospectionELResolver;
import io.micronaut.el.resolver.ReflectiveMethodELResolver;
import io.micronaut.inject.ProxyBeanDefinition;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.MethodNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The compile-time generated counterparts of the interpreted regressions of the executable method resolution.
 */
class ExecutableMethodExpressionsTest {

    private final ApplicationContext beanContext = ApplicationContext.run();
    private final IntrospectedGreeter introspected = beanContext.getBean(IntrospectedGreeter.class);
    private final AdvisedGreeter advised = beanContext.getBean(AdvisedGreeter.class);
    private final CompiledELContext context = new CompiledELContext(beanContext)
        .setBean("greeter", beanContext.getBean(Greeter.class))
        .setBean("introspected", introspected)
        .setBean("advised", advised)
        .setBean("plain", new Plain());

    @AfterEach
    void close() {
        beanContext.close();
    }

    @Test
    void anExecutableMethodIsInvokedWithoutAnIntrospection() {
        assertTrue(BeanIntrospector.SHARED.findIntrospection(Greeter.class).isEmpty(),
            "the bean of the scenario must not carry an introspection");
        assertEquals("hello world", ExecutableMethodExpressions$ELExpressions.GREET.getValue(context));
    }

    @Test
    void theMethodExpressionOfTheScenarioInvokesTheMethod() {
        MethodExpression expression = ExpressionFactory.newInstance()
            .createMethodExpression(context, "${greeter.greet('world')}", String.class, new Class<?>[0]);
        assertEquals("hello world", expression.invoke(context, new Object[0]));
        assertEquals("hello world",
            ExecutableMethodExpressions$ELExpressions.GREET_METHOD.invoke(context, new Object[0]));
    }

    @Test
    void theArgumentsAreCoercedAndTheOverloadIsSelectedAsTheIntrospectionPathDoes() {
        assertEquals((Object) 42, ExecutableMethodExpressions$ELExpressions.TWICE.getValue(context));
        assertEquals("string", ExecutableMethodExpressions$ELExpressions.SELECT.getValue(context));
    }

    @Test
    void anAmbiguousCallReportsTheSameExceptionAsTheOtherResolvers() {
        assertThrows(MethodNotFoundException.class,
            () -> ExecutableMethodExpressions$ELExpressions.AMBIGUOUS.getValue(context));
    }

    @Test
    void anIntrospectedTypeKeepsResolvingThroughItsIntrospection() {
        assertEquals("hi world", ExecutableMethodExpressions$ELExpressions.INTROSPECTED.getValue(context));
        assertEquals(1, introspected.getCalls(), "the method must not be resolved, and invoked, twice");
    }

    @Test
    void aMethodOnAnAdvisedBeanResolvesToTheExecutableMethodOfTheDeclaringClass() {
        assertTrue(beanContext.getBeanDefinition(advised.getClass()) instanceof ProxyBeanDefinition,
            "the definition of the runtime class of the bean must be a proxy definition");
        // one interception, not two: the executable method that was found is the one of the declaring class,
        // and invoking it on the proxy runs the interceptor chain once
        assertEquals("hello world!", ExecutableMethodExpressions$ELExpressions.ADVISED.getValue(context));

        ELMethod resolved = new ExecutableMethodELExecutor(beanContext)
            .resolve(context, advised, "greet", null, new Object[]{"world"});
        assertTrue(resolved != null && resolved.identity().startsWith(AdvisedGreeter.class.getName() + "#greet"),
            () -> "expected the method of the declaring class but got " + (resolved == null ? null : resolved.identity()));
    }

    @Test
    void aTypeThatIsNeitherIntrospectedNorABeanStillResolvesReflectively() {
        assertEquals("WORLD", ExecutableMethodExpressions$ELExpressions.PLAIN.getValue(context));
        assertEquals("hidden world", ExecutableMethodExpressions$ELExpressions.HIDDEN.getValue(context));

        ExecutableMethodELExecutor alone = new ExecutableMethodELExecutor(beanContext);
        context.setPropertyResolved(false);
        assertNull(alone.invoke(context, new Plain(), "shout", null, new Object[]{"world"}));
        assertFalse(context.isPropertyResolved());
    }

    @Test
    void theResolverSitsAfterTheIntrospectionsAndBeforeTheReflectiveInvocation() {
        List<String> chain = ELResolvers.standardResolvers(beanContext).stream()
            .map(resolver -> resolver.getClass().getName())
            .toList();
        assertTrue(chain.indexOf(IntrospectionELResolver.class.getName())
            < chain.indexOf(ExecutableMethodELExecutor.class.getName()));
        assertTrue(chain.indexOf(ExecutableMethodELExecutor.class.getName())
            < chain.indexOf(ReflectiveMethodELResolver.class.getName()));
        // without a bean context the chain is the one it was
        assertFalse(ELResolvers.standardResolvers().stream()
            .anyMatch(resolver -> resolver instanceof ExecutableMethodELExecutor));
    }

    @Test
    void theServiceFormReadsTheRegistryOfTheContext() {
        Greeter greeter = beanContext.getBean(Greeter.class);
        // the no-argument form the service loader constructs, which CompiledELContext registered the registry
        // on the context for
        ELMethod resolved = new ExecutableMethodELExecutor()
            .resolve(context, greeter, "greet", null, new Object[]{"world"});
        assertNotNull(resolved);
        assertEquals("hello world", resolved.invoke(context, greeter, new Object[]{"world"}));

        // a context carrying no registry is one the service form declines
        assertNull(new ExecutableMethodELExecutor()
            .resolve(new CompiledELContext(), greeter, "greet", null, new Object[]{"world"}));
    }
}
