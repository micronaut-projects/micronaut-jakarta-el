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
package io.micronaut.el.interpreter;

import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.ContributedELMethodExecutor;
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELMethodContributor;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.ELMethodRegistry;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.MethodNotFoundException;
import jakarta.el.StandardELContext;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A contributed executor makes a type callable from a runtime-parsed expression without reflection, which is
 * the configuration the interpreter supports on its own.
 */
class ELMethodContributorTest {

    static ExpressionFactory factory(ELMethodContributor... contributors) {
        List<ELMethodExecutor> executors = List.of(new ContributedELMethodExecutor(List.of(contributors)));
        return new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser(executors));
    }

    static Object evaluate(ExpressionFactory factory, ELContext context, String expression) {
        return factory.createValueExpression(context, expression, Object.class).getValue(context);
    }

    static ELContext contextWith(ExpressionFactory factory, String name, Object value) {
        StandardELContext context = new StandardELContext(factory);
        context.getVariableMapper().setVariable(name,
            factory.createValueExpression(value, Object.class));
        return context;
    }

    @Test
    void aContributedMethodIsCallableWithoutReflection() {
        ExpressionFactory factory = factory(registry -> registry
            .method(Greeter.class, "greet", String.class, String.class, Greeter::greet));
        ELContext context = contextWith(factory, "greeter", new Greeter("ada"));

        assertEquals("hello world, ada", evaluate(factory, context, "${greeter.greet('world')}"));
    }

    @Test
    void aMethodThatWasNotContributedIsNotFound() {
        ExpressionFactory factory = factory(registry -> registry
            .method(Greeter.class, "greet", String.class, String.class, Greeter::greet));
        ELContext context = contextWith(factory, "greeter", new Greeter("ada"));

        MethodNotFoundException e = assertThrows(MethodNotFoundException.class,
            () -> evaluate(factory, context, "${greeter.count()}"));
        assertTrue(e.getMessage().contains("count"), e.getMessage());
    }

    @Test
    void theArgumentsAreCoercedToTheDeclaredParameterTypes() {
        ExpressionFactory factory = factory(registry -> registry
            .method(Greeter.class, "greet", String.class, String.class, Greeter::greet));
        ELContext context = contextWith(factory, "greeter", new Greeter("ada"));

        // the argument is a Long, the parameter a String: section 1.23 coerces it before the invocation
        assertEquals("hello 42, ada", evaluate(factory, context, "${greeter.greet(42)}"));
    }

    @Test
    void anOverloadIsSelectedAsTheSpecificationRequires() {
        ExpressionFactory factory = factory(registry -> registry
            .method(Greeter.class, "greet", String.class, String.class, Greeter::greet)
            .method(Greeter.class, "greet", String.class, Integer.class, Greeter::greet));
        ELContext context = contextWith(factory, "greeter", new Greeter("ada"));

        assertEquals("hello world, ada", evaluate(factory, context, "${greeter.greet('world')}"));
        assertEquals("hello hello ", evaluate(factory, context, "${greeter.greet(2)}"));
    }

    @Test
    void aVariableArityMethodPacksItsTrailingArguments() {
        ExpressionFactory factory = factory(registry -> registry
            .method(Greeter.class, "join", String.class, new Class<?>[]{String.class, String[].class}, true,
                (context, base, arguments) ->
                    ((Greeter) base).join((String) arguments[0], (String[]) arguments[1])));
        ELContext context = contextWith(factory, "greeter", new Greeter("ada"));

        assertEquals("a-b-c", evaluate(factory, context, "${greeter.join('-', 'a', 'b', 'c')}"));
        assertEquals("", evaluate(factory, context, "${greeter.join('-')}"));
    }

    @Test
    void aContributedStaticMethodAndConstructorAreCallable() {
        ExpressionFactory factory = factory(registry -> registry
            .staticMethod(Math.class, "abs", int.class, int.class, Math::abs)
            .constructor(Greeter.class, String.class, Greeter::new)
            .method(Greeter.class, "getName", String.class, Greeter::getName));
        StandardELContext context = new StandardELContext(factory);
        context.getImportHandler().importClass(Greeter.class.getName());

        assertEquals(7, evaluate(factory, context, "${Math.abs(-7)}"));
        assertEquals("ada", evaluate(factory, context, "${Greeter('ada').name}"));
    }

    @Test
    void aContributedFunctionReplacesTheFunctionMapperLookup() {
        ExpressionFactory factory = factory(registry -> registry
            .function("greet", "twice", Greeter.class, "twice", String.class, String.class,
                whom -> whom + whom));
        StandardELContext context = new StandardELContext(factory);

        assertEquals("abab", evaluate(factory, context, "${greet:twice('ab')}"));
    }

    @Test
    void aResolvedMethodIsKeptByTheCallSiteAndInvokedAgain() {
        AtomicInteger invocations = new AtomicInteger();
        ELMethodRegistry registry = new ELMethodRegistry()
            .method(Greeter.class, "greet", String.class, String.class, (Greeter greeter, String whom) -> {
                invocations.incrementAndGet();
                return greeter.greet(whom);
            });
        CountingExecutor counting = new CountingExecutor(registry.build(0));
        ExpressionFactory factory = new CompiledExpressionFactory(List.of(),
            new InterpretingELExpressionParser(List.of(counting)));
        ELContext context = contextWith(factory, "greeter", new Greeter("ada"));

        ValueExpression expression = factory.createValueExpression(context, "${greeter.greet('world')}",
            Object.class);
        for (int i = 0; i < 5; i++) {
            assertEquals("hello world, ada", expression.getValue(context));
        }

        // the method runs on every evaluation, but is selected only once: the call site kept it
        assertEquals(5, invocations.get());
        assertEquals(1, counting.resolutions.get());
    }

    @Test
    void anOverloadedCallSiteIsResolvedOnEveryEvaluation() {
        ELMethodRegistry registry = new ELMethodRegistry()
            .method(Greeter.class, "greet", String.class, String.class, Greeter::greet)
            .method(Greeter.class, "greet", String.class, Integer.class, Greeter::greet);
        CountingExecutor counting = new CountingExecutor(registry.build(0));
        ExpressionFactory factory = new CompiledExpressionFactory(List.of(),
            new InterpretingELExpressionParser(List.of(counting)));
        ELContext context = contextWith(factory, "greeter", new Greeter("ada"));

        ValueExpression expression = factory.createValueExpression(context, "${greeter.greet('world')}",
            Object.class);
        for (int i = 0; i < 5; i++) {
            assertEquals("hello world, ada", expression.getValue(context));
        }

        // the arguments could select the other overload, so the selection is not kept
        assertEquals(5, counting.resolutions.get());
    }

    @Test
    void aMethodOfASingleRegistrationIsReusableAndAnOverloadedOneIsNot() {
        ELMethodRegistry single = new ELMethodRegistry()
            .method(Greeter.class, "count", int.class, Greeter::count);
        ELMethodRegistry overloaded = new ELMethodRegistry()
            .method(Greeter.class, "greet", String.class, String.class, Greeter::greet)
            .method(Greeter.class, "greet", String.class, Integer.class, Greeter::greet);
        StandardELContext context = new StandardELContext(factory());
        Greeter greeter = new Greeter("ada");

        ELMethod one = single.build(0).resolve(context, greeter, "count", null, new Object[0]);
        ELMethod many = overloaded.build(0).resolve(context, greeter, "greet", null, new Object[]{"x"});

        assertNotNull(one);
        assertNotNull(many);
        assertTrue(one.isReusable(), "a name with one registration cannot select another overload");
        assertTrue(!many.isReusable(), "an overloaded name is selected from the arguments of each call");
    }

    @Test
    void aContributedMethodCarriesTheMetadataOfAMethodExpression() {
        ExpressionFactory factory = factory(registry -> registry
            .method(Greeter.class, "greet", String.class, String.class, Greeter::greet));
        ELContext context = contextWith(factory, "greeter", new Greeter("ada"));

        MethodExpression expression = factory.createMethodExpression(context, "${greeter.greet}",
            String.class, new Class<?>[]{String.class});

        assertEquals("greet", expression.getMethodInfo(context).getName());
        assertEquals(String.class, expression.getMethodInfo(context).getReturnType());
        assertEquals("hello world, ada", expression.invoke(context, new Object[]{"world"}));
    }

    @Test
    void theIdentityOfAContributedMethodIsTheOneTheCompilerGives() {
        assertEquals(Greeter.class.getName() + "#greet(java.lang.String)",
            ELMethod.identity(Greeter.class, "greet", new Class<?>[]{String.class}));
    }

    @Test
    void aMethodRegisteredOnAnInterfaceIsCallableOnItsImplementations() {
        ExpressionFactory factory = factory(registry -> registry
            .method(CharSequence.class, "describe", String.class,
                (CharSequence value) -> "length " + value.length()));
        ELContext context = contextWith(factory, "text", "abcd");

        assertEquals("length 4", evaluate(factory, context, "${text.describe()}"));
    }

    @Test
    void aVariableArityRegistrationMustDeclareAnArray() {
        ELMethodRegistry registry = new ELMethodRegistry();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> registry.method(Greeter.class, "join", String.class, new Class<?>[]{String.class}, true,
                (context, base, arguments) -> null));
        assertTrue(e.getMessage().contains("array"), e.getMessage());
    }

    @Test
    void anArgumentListThatFitsNoOverloadOfARegisteredNameIsAnError() {
        ExpressionFactory factory = factory(registry -> registry
            .method(Greeter.class, "greet", String.class, String.class, Greeter::greet));
        ELContext context = contextWith(factory, "greeter", new Greeter("ada"));

        // the name is registered, so the call is wrong rather than unhandled
        ELException e = assertThrows(ELException.class,
            () -> evaluate(factory, context, "${greeter.greet('a', 'b')}"));
        assertInstanceOf(MethodNotFoundException.class, e);
    }

    @Test
    void nothingIsContributedWhenNoContributorIsRegistered() {
        ExpressionFactory factory = factory();
        ELContext context = contextWith(factory, "greeter", new Greeter("ada"));

        assertThrows(MethodNotFoundException.class,
            () -> evaluate(factory, context, "${greeter.greet('world')}"));
        // a property still resolves, through the standard resolver chain
        assertEquals("ada", evaluate(factory, context, "${greeter.name}"));
    }

    @Test
    void anEmptyRegistryResolvesNothing() {
        ELMethodExecutor executor = new ELMethodRegistry().build(0);
        StandardELContext context = new StandardELContext(factory());

        assertNull(executor.resolve(context, new Greeter("ada"), "greet", null, new Object[]{"x"}));
        assertNull(executor.resolveFunction(context, "any", "thing"));
        assertTrue(new ELMethodRegistry().isEmpty());
    }

    /**
     * Counts how often a call site asks for a method to be resolved.
     */
    private static final class CountingExecutor implements ELMethodExecutor {

        private final ELMethodExecutor delegate;
        private final AtomicInteger resolutions = new AtomicInteger();

        private CountingExecutor(ELMethodExecutor delegate) {
            this.delegate = delegate;
        }

        @Override
        public ELMethod resolve(ELContext context,
                                Object base,
                                Object method,
                                io.micronaut.core.type.Argument<?>[] argumentTypes,
                                Object[] arguments) {
            resolutions.incrementAndGet();
            return delegate.resolve(context, base, method, argumentTypes, arguments);
        }

        @Override
        public ELMethod resolveFunction(ELContext context, String prefix, String localName) {
            return delegate.resolveFunction(context, prefix, localName);
        }
    }
}
