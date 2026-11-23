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
package io.micronaut.el.interpreter.reflection;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.runtime.ELLambdas;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.LambdaExpression;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The coercion of a lambda expression to an interface nothing described in advance, which needs the method to
 * be discovered at runtime and therefore lives in this module.
 */
class ReflectiveELProxyFactoryTest {

    interface UnannotatedFunction {

        String apply(String value);

        @Override
        boolean equals(Object object);
    }

    @Test
    void lambdaCoercesToAnUnannotatedFunctionalInterface() {
        CompiledELContext context = new CompiledELContext();
        LambdaExpression lambda = ELLambdas.create(context, List.of("value"),
            evaluated -> evaluated.getLambdaArgument("value"));

        UnannotatedFunction function = ELSupport.coerceToType(context, lambda, UnannotatedFunction.class);

        assertEquals("lambda", function.apply("lambda"));
        assertTrue(function.equals(function));
        assertFalse(function.equals(new Object()));
        assertTrue(Proxy.isProxyClass(function.getClass()));
    }

    @Test
    void aKnownFunctionalInterfaceIsStillImplementedWithoutAProxy() {
        CompiledELContext context = new CompiledELContext();
        java.util.function.Supplier<?> supplier = ELSupport.coerceToType(context,
            ELLambdas.create(context, List.of(), evaluated -> "supplied"), java.util.function.Supplier.class);

        assertEquals("supplied", supplier.get());
        assertFalse(Proxy.isProxyClass(supplier.getClass()));
    }

    interface Defaulted {

        int apply(int value);

        default int twice(int value) {
            return apply(apply(value));
        }
    }

    @Test
    void aDefaultMethodOfAPlatformInterfaceIsInvoked() {
        // java.util.function is not open to this module, so a private lookup in it is refused
        CompiledELContext context = new CompiledELContext();
        LambdaExpression lambda = ELLambdas.create(context, List.of("x"),
            evaluated -> ((Number) evaluated.getLambdaArgument("x")).intValue() * 10);

        IntUnaryOperator operator = ELSupport.coerceToType(context, lambda, IntUnaryOperator.class);

        assertTrue(Proxy.isProxyClass(operator.getClass()));
        assertEquals(11, operator.andThen(x -> x + 1).applyAsInt(1));
        assertEquals(100, operator.compose(x -> x * 10).applyAsInt(1));
    }

    @Test
    void aDefaultMethodOfAnApplicationInterfaceIsInvoked() {
        CompiledELContext context = new CompiledELContext();
        LambdaExpression lambda = ELLambdas.create(context, List.of("x"),
            evaluated -> ((Number) evaluated.getLambdaArgument("x")).intValue() + 1);

        assertEquals(3, ELSupport.coerceToType(context, lambda, Defaulted.class).twice(1));
    }

    interface Task {

        void run();

        default String describe() {
            return "task";
        }
    }

    @Test
    void aVoidMethodWithoutParametersRunsTheLambdaAndReturnsNothing() {
        CompiledELContext context = new CompiledELContext();
        List<String> runs = new java.util.ArrayList<>();
        LambdaExpression lambda = ELLambdas.create(context, List.of(), evaluated -> {
            runs.add("run");
            return "ignored";
        });

        // coerced without a context, the lambda expression is invoked with the context it carries
        Task task = ELSupport.coerce(lambda, Task.class);
        task.run();
        task.run();

        assertTrue(Proxy.isProxyClass(task.getClass()));
        assertEquals(List.of("run", "run"), runs);
        assertEquals("task", task.describe());
        assertEquals(System.identityHashCode(task), task.hashCode());
        assertEquals(lambda.toString(), task.toString());
    }

    @Test
    void theProxyIsInvokedConcurrentlyWithAContextOfItsOwnPerCall() {
        CompiledELContext context = new CompiledELContext();
        // the enclosing lambda argument is read from the context that coerced the lambda
        context.enterLambdaScope(Map.of("offset", 7));
        try {
            LambdaExpression lambda = ELLambdas.create(context, List.of("x"), evaluated -> {
                evaluated.setPropertyResolved(false);
                Object value = ((Number) evaluated.getLambdaArgument("x")).intValue()
                    + ((Number) evaluated.getLambdaArgument("offset")).intValue();
                evaluated.setPropertyResolved(true);
                return value;
            });
            Defaulted function = ELSupport.coerceToType(context, lambda, Defaulted.class);

            int[] results = IntStream.range(0, 200_000).parallel().map(function::apply).toArray();

            for (int i = 0; i < results.length; i++) {
                assertEquals(i + 7, results[i]);
            }
            assertFalse(context.isPropertyResolved());
        } finally {
            context.exitLambdaScope();
        }
    }
}
