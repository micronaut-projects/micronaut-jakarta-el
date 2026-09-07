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
}
