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

    @Test
    void withoutAContextCarryingARegistryTheExecutorDeclines() {
        CompiledELContext unregistered = beans(new CompiledELContext());
        assertThrows(MethodNotFoundException.class, () -> unreflectedFactory
            .createValueExpression(unregistered, "${greeter.greet('world')}", Object.class)
            .getValue(unregistered));
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
