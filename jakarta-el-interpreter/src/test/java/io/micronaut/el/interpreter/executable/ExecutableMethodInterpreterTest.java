/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.el.interpreter.executable;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanDefinitionRegistry;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.interpreter.InterpretingELExpressionParser;
import io.micronaut.el.resolver.ELMethodDiagnostics;
import io.micronaut.el.resolver.ExecutableMethodELExecutor;
import io.micronaut.el.resolver.IntrospectionELResolver;
import io.micronaut.inject.ProxyBeanDefinition;
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The interpreted regressions of the executable method resolution: a method the compiler was asked to make
 * executable is reachable from an expression parsed at runtime, without the type also carrying a bean
 * introspection, and without the reflective executor being on the classpath.
 *
 * <p>Every expression here has a compile-time generated counterpart, with the same text and the same expected
 * result, in {@code io.micronaut.el.test.executable.ExecutableMethodExpressions} of the Java test suite.</p>
 */
class ExecutableMethodInterpreterTest {

    /**
     * The package of the optional reflective executor, which this module puts on the test runtime classpath and
     * which the executors of {@link #unreflectedExecutors()} deliberately leave out.
     */
    private static final String REFLECTION = "io.micronaut.el.interpreter.reflection.";

    private final ApplicationContext beanContext = ApplicationContext.run();
    private final IntrospectedGreeter introspected = beanContext.getBean(IntrospectedGreeter.class);
    private final AdvisedGreeter advised = beanContext.getBean(AdvisedGreeter.class);

    /**
     * The context of an application that wired the executable methods into its resolver chain, which registers
     * the registry on itself as well.
     */
    private final ELContext context = beans(new CompiledELContext(beanContext));

    /**
     * The context of an application that wired nothing: the standard chain, with the registry registered on the
     * context, which is all the executor loaded as a service needs.
     */
    private final CompiledELContext serviceContext = registered(beans(new CompiledELContext()));

    private final ExpressionFactory factory =
        new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser());

    private final ExpressionFactory unreflectedFactory =
        new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser(unreflectedExecutors()));

    /**
     * The executors declared as services, minus the reflective one, which is how a deployment that does not
     * have {@code micronaut-jakarta-el-interpreter-reflection} on its classpath resolves methods. The module is
     * a test runtime dependency here, so leaving it out of the list is what standing in for its absence looks
     * like.
     */
    private static List<ELMethodExecutor> unreflectedExecutors() {
        List<ELMethodExecutor> all = SoftServiceLoader.load(ELMethodExecutor.class).collectAll();
        assertTrue(all.stream().anyMatch(executor -> executor.getClass().getName().startsWith(REFLECTION)),
            "the reflective executor must be on the classpath for leaving it out to mean anything");
        return all.stream()
            .filter(executor -> !executor.getClass().getName().startsWith(REFLECTION))
            .toList();
    }

    private CompiledELContext beans(CompiledELContext target) {
        return target
            .setBean("greeter", beanContext.getBean(Greeter.class))
            .setBean("introspected", introspected)
            .setBean("advised", advised)
            .setBean("plain", new Plain());
    }

    /**
     * Registers the bean context on the EL context, which is everything an application has to do for the
     * executor loaded as a service to find the executable methods of its beans.
     */
    private CompiledELContext registered(CompiledELContext target) {
        target.putContext(BeanDefinitionRegistry.class, beanContext);
        return target;
    }

    @AfterEach
    void close() {
        beanContext.close();
    }

    private Object value(String expression) {
        return factory.createValueExpression(context, expression, Object.class).getValue(context);
    }

    private Object unreflected(String expression) {
        return unreflectedFactory.createValueExpression(serviceContext, expression, Object.class)
            .getValue(serviceContext);
    }

    @Test
    void theExecutorIsRegisteredAsAServiceAfterTheIntrospectionsAndBeforeTheReflectiveOne() {
        List<ELMethodExecutor> services = SoftServiceLoader.load(ELMethodExecutor.class).collectAll();
        ELMethodExecutor executable = services.stream()
            .filter(ExecutableMethodELExecutor.class::isInstance)
            .findFirst()
            .orElseThrow(() -> new AssertionError("the executor must be declared as a service"));
        ELMethodExecutor introspections = services.stream()
            .filter(IntrospectionELResolver.class::isInstance)
            .findFirst()
            .orElseThrow();
        ELMethodExecutor reflective = services.stream()
            .filter(executor -> executor.getClass().getName().startsWith(REFLECTION))
            .findFirst()
            .orElseThrow();
        assertTrue(introspections.getOrder() < executable.getOrder());
        assertTrue(executable.getOrder() < reflective.getOrder());
    }

    @Test
    void anExecutableMethodIsInvokedWithoutAnIntrospection() {
        assertTrue(BeanIntrospector.SHARED.findIntrospection(Greeter.class).isEmpty(),
            "the bean of the scenario must not carry an introspection");
        assertEquals("hello world", value("${greeter.greet('world')}"));
    }

    @Test
    void anExecutableMethodIsInvokedWithTheReflectiveExecutorAbsent() {
        assertTrue(BeanIntrospector.SHARED.findIntrospection(Greeter.class).isEmpty(),
            "the bean of the scenario must not carry an introspection");
        assertEquals("hello world", unreflected("${greeter.greet('world')}"));
    }

    /**
     * The failure that costs the most to diagnose: the method exists, is public and is executable, and the only
     * thing missing is the bean context the executor reads from the {@link ELContext}. Reporting the name of
     * the method alone reads as a method that plainly exists not being found, so the message has to say that
     * the executable methods were never consulted, and to name the two ways out.
     */
    @Test
    void withoutAContextCarryingARegistryTheExecutorDeclines() {
        CompiledELContext unregistered = beans(new CompiledELContext());
        String message = assertThrows(MethodNotFoundException.class, () -> unreflectedFactory
            .createValueExpression(unregistered, "${greeter.greet('world')}", Object.class)
            .getValue(unregistered)).getMessage();
        assertTrue(message.startsWith("Cannot find the method 'greet' of " + Greeter.class.getName()
            + " accepting 1 argument(s)."), message);
        assertTrue(message.contains("No bean context is registered in this ELContext"), message);
        assertTrue(message.contains("context.putContext(BeanDefinitionRegistry.class, beanContext)"), message);
        assertTrue(message.contains("new CompiledELContext(beanContext)"), message);
        assertTrue(message.contains("add the micronaut-jakarta-el-interpreter-reflection module"), message);
        // the context is the cause, so the definition must not be blamed for a method it does carry
        assertFalse(message.contains("carries no executable method named 'greet'"), message);
    }

    /**
     * The other side of the same distinction: the bean context is registered, so the executable methods of the
     * definition were read, and what is missing is {@code @Executable} on the method itself.
     */
    @Test
    void withARegisteredContextADefinitionLackingTheMethodIsReportedAsSuch() {
        String message = assertThrows(MethodNotFoundException.class,
            () -> unreflected("${greeter.hidden('world')}")).getMessage();
        assertTrue(message.contains(Greeter.class.getName()
            + " is a bean of the bean context registered in this ELContext"), message);
        assertTrue(message.contains("carries no executable method named 'hidden'"), message);
        assertTrue(message.contains("annotate the method with @Executable"), message);
        assertTrue(message.contains("annotate it with @Introspected"), message);
        assertTrue(message.contains("add the micronaut-jakarta-el-interpreter-reflection module"), message);
        assertFalse(message.contains("No bean context is registered in this ELContext"), message);
    }

    /**
     * A type that is not a bean of the registered context at all is reported as such, rather than as a bean
     * whose method is missing an annotation.
     */
    @Test
    void withARegisteredContextATypeThatIsNotABeanOfItIsReportedAsSuch() {
        String message = assertThrows(MethodNotFoundException.class,
            () -> unreflected("${plain.shout('world')}")).getMessage();
        assertTrue(message.contains("has no bean definition for " + Plain.class.getName()), message);
        assertTrue(message.contains("annotate it with @Introspected"), message);
        assertTrue(message.contains("add the micronaut-jakarta-el-interpreter-reflection module"), message);
    }

    /**
     * The arity, which the name alone does not distinguish: the definition carries the method, so the
     * signatures it carries are reported next to the number of arguments that selected none of them.
     */
    @Test
    void aNameThatIsCarriedButNotWithThatArityReportsTheSignatures() {
        String message = ELMethodDiagnostics.notFound(serviceContext, beanContext.getBean(Greeter.class),
            "greet", new Object[]{"a", "b"}, unreflectedExecutors()).getMessage();
        assertTrue(message.contains("accepting 2 argument(s)"), message);
        assertTrue(message.contains("The bean definition of " + Greeter.class.getName()
            + " carries greet(java.lang.String), but the 2 argument(s) given select none of them"), message);
        assertFalse(message.contains("carries no executable method named 'greet'"), message);
    }

    /**
     * An introspected type is described through its introspection as well, so that the remedy named is the one
     * of the description the method would have been dispatched from.
     */
    @Test
    void anIntrospectedTypeIsDescribedThroughItsIntrospectionToo() {
        String message = ELMethodDiagnostics.notFound(serviceContext, introspected, "missing",
            new Object[]{"world"}, unreflectedExecutors()).getMessage();
        assertTrue(message.contains("Its bean introspection carries no method named 'missing'"), message);
        assertTrue(message.contains("so that it enters the introspection"), message);
    }

    /**
     * With the reflective executor present the module is not named as a remedy: it was consulted, so no public
     * method of that name accepts the arguments, and pointing at the module would send the reader nowhere.
     */
    @Test
    void withTheReflectiveExecutorPresentTheModuleIsNotNamedAsARemedy() {
        List<ELMethodExecutor> all = SoftServiceLoader.load(ELMethodExecutor.class).collectAll();
        String message = ELMethodDiagnostics.notFound(serviceContext, beanContext.getBean(Greeter.class),
            "missing", new Object[]{"world"}, all).getMessage();
        assertTrue(message.contains("The reflective executor was consulted as well"), message);
        assertFalse(message.contains("add the micronaut-jakarta-el-interpreter-reflection module"), message);
    }

    @Test
    void theMethodExpressionOfTheScenarioInvokesTheMethod() {
        assertEquals("hello world", factory
            .createMethodExpression(context, "${greeter.greet('world')}", String.class, new Class<?>[0])
            .invoke(context, new Object[0]));
        assertEquals("hello world", unreflectedFactory
            .createMethodExpression(serviceContext, "${greeter.greet('world')}", String.class, new Class<?>[0])
            .invoke(serviceContext, new Object[0]));
    }

    @Test
    void theArgumentsAreCoercedAndTheOverloadIsSelectedAsTheIntrospectionPathDoes() {
        assertEquals(42, value("${greeter.twice('21')}"));
        assertEquals("string", value("${greeter.select('1')}"));
        assertEquals(42, unreflected("${greeter.twice('21')}"));
        assertEquals("string", unreflected("${greeter.select('1')}"));
    }

    @Test
    void anAmbiguousCallReportsTheSameExceptionAsTheOtherResolvers() {
        assertThrows(MethodNotFoundException.class, () -> value("${greeter.ambiguous(1)}"));
        assertThrows(MethodNotFoundException.class, () -> unreflected("${greeter.ambiguous(1)}"));
    }

    @Test
    void anIntrospectedTypeKeepsResolvingThroughItsIntrospection() {
        assertEquals("hi world", value("${introspected.greet('world')}"));
        assertEquals(1, introspected.getCalls(), "the method must not be resolved, and invoked, twice");
    }

    @Test
    void anIntrospectedTypeKeepsResolvingThroughItsIntrospectionWithoutReflection() {
        assertEquals("hi world", unreflected("${introspected.greet('world')}"));
        assertEquals(1, introspected.getCalls(), "the method must not be resolved, and invoked, twice");
    }

    @Test
    void aMethodOnAnAdvisedBeanResolvesToTheExecutableMethodOfTheDeclaringClass() {
        assertTrue(beanContext.getBeanDefinition(advised.getClass()) instanceof ProxyBeanDefinition,
            "the definition of the runtime class of the bean must be a proxy definition");
        // one interception, not two: the executable method that was found is the one of the declaring class
        assertEquals("hello world!", value("${advised.greet('world')}"));
        assertEquals("hello world!", unreflected("${advised.greet('world')}"));
    }

    @Test
    void aTypeThatIsNeitherIntrospectedNorABeanStillResolvesReflectively() {
        assertEquals("WORLD", value("${plain.shout('world')}"));
        assertEquals("hidden world", value("${greeter.hidden('world')}"));
    }

    @Test
    void aTypeThatIsNeitherIntrospectedNorABeanReportsTheSameExceptionWithoutTheReflectiveExecutor() {
        assertThrows(MethodNotFoundException.class, () -> unreflected("${plain.shout('world')}"));
        assertThrows(MethodNotFoundException.class, () -> unreflected("${greeter.hidden('world')}"));
    }
}
