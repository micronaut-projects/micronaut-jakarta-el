package io.micronaut.el.test.executable;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanDefinitionRegistry;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.ELMethod;
import io.micronaut.el.resolver.ELResolverChain;
import io.micronaut.el.resolver.ELResolvers;
import io.micronaut.el.resolver.ExecutableMethodELExecutor;
import io.micronaut.el.resolver.IntrospectionELResolver;
import io.micronaut.el.resolver.ReflectiveMethodELResolver;
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.inject.ProxyBeanDefinition;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodExpression;
import jakarta.el.MethodNotFoundException;
import jakarta.el.VariableMapper;
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

    /**
     * The compiled counterpart of the diagnostics the interpreted path reports. A compiled expression dispatches
     * through {@code ELResolution.invoke}, which reports the failure of a chain that resolved nothing, so the
     * same distinction has to be made there: a context carrying no bean context is one where the executable
     * methods were never read, and a registered one that has the type but not the method is a missing
     * {@code @Executable}.
     *
     * <p>The chain here is a narrow one, an application that wired the introspections alone. The standard chain
     * ends in the reflective resolvers of the specification, and {@code BeanELResolver} reports a method of its
     * own that it did not find, so it is a chain that stops short of them that reaches this message.</p>
     */
    @Test
    void aMethodThatTheChainDoesNotResolveSaysWhy() {
        Greeter greeter = beanContext.getBean(Greeter.class);
        ELContext narrow = introspectionsOnly();

        String unregistered = assertThrows(MethodNotFoundException.class,
            () -> ELResolution.invoke(narrow, greeter, "greet", "world")).getMessage();
        assertTrue(unregistered.startsWith("Cannot find the method 'greet' of " + Greeter.class.getName()
            + " accepting 1 argument(s)."), unregistered);
        assertTrue(unregistered.contains("No bean context is registered in this ELContext"), unregistered);
        assertTrue(unregistered.contains("context.putContext(BeanDefinitionRegistry.class, beanContext)"),
            unregistered);
        // the remedy of the interpreted path is the executors, not a chain, so it is not named here
        assertFalse(unregistered.contains("add the micronaut-jakarta-el-interpreter-reflection module"),
            unregistered);

        narrow.putContext(BeanDefinitionRegistry.class, beanContext);
        String registered = assertThrows(MethodNotFoundException.class,
            () -> ELResolution.invoke(narrow, greeter, "missing", "world")).getMessage();
        assertTrue(registered.contains(Greeter.class.getName()
            + " is a bean of the bean context registered in this ELContext"), registered);
        assertTrue(registered.contains("carries no executable method named 'missing'"), registered);
        assertTrue(registered.contains("annotate it with @Introspected"), registered);
        assertFalse(registered.contains("No bean context is registered in this ELContext"), registered);
    }

    /**
     * A context whose chain is the bean introspections alone, which declines a type that carries none instead
     * of throwing a method of its own the way the reflective resolvers of the specification do.
     */
    private static ELContext introspectionsOnly() {
        return new ELContext() {

            private final ELResolver resolver = new ELResolverChain(new IntrospectionELResolver());

            @Override
            public ELResolver getELResolver() {
                return resolver;
            }

            @Override
            public FunctionMapper getFunctionMapper() {
                return null;
            }

            @Override
            public VariableMapper getVariableMapper() {
                return null;
            }
        };
    }
}
