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
package io.micronaut.el;

import io.micronaut.el.runtime.ELLambdas;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An application keeps the coercion of a lambda expression to an interface of its own off the reflective path
 * by registering how to implement it, in the contributor it declares its callable surface in.
 */
class ContributedFunctionalInterfaceTest {

    /**
     * An interface of the application, which nothing but the application knows how to implement.
     */
    public interface Decorator {

        String decorate(String value);

        default String twice(String value) {
            return decorate(decorate(value));
        }
    }

    /**
     * An interface nothing registered, which still reaches the proxy.
     */
    public interface Undecorated {

        String apply(String value);
    }

    /**
     * The contributor, named in {@code META-INF/services/io.micronaut.el.ELMethodContributor}.
     */
    public static final class Decorators implements ELMethodContributor {

        @Override
        public void contribute(ELMethodRegistry registry) {
            registry.functionalInterface(Decorator.class,
                (context, lambda) -> value -> String.valueOf(
                    context == null ? lambda.invoke(value) : lambda.invoke(context, value)));
        }
    }

    @Test
    void theRegisteredImplementationIsUsedInsteadOfAProxy() {
        CompiledELContext context = new CompiledELContext();
        LambdaExpression lambda = ELLambdas.create(context, List.of("value"),
            evaluated -> "<" + evaluated.getLambdaArgument("value") + ">");

        Decorator decorator = ELSupport.coerceToType(context, lambda, Decorator.class);

        assertEquals("<el>", decorator.decorate("el"));
        // the default method comes from the interface itself, not from an invocation handler
        assertEquals("<<el>>", decorator.twice("el"));
        assertEquals("<el>", decorator.decorate("el"));
    }

    @Test
    void anInterfaceNobodyRegisteredIsNotCoercedWithoutTheReflectionModule() {
        CompiledELContext context = new CompiledELContext();
        LambdaExpression lambda = ELLambdas.create(context, List.of("value"),
            evaluated -> evaluated.getLambdaArgument("value"));

        // nothing here can discover the method of an interface it was never told about, and this module does
        // not reflect: the failure says what to do about it
        ELException e = assertThrows(ELException.class,
            () -> ELSupport.coerceToType(context, lambda, Undecorated.class));
        assertTrue(e.getMessage().contains("ELMethodRegistry.functionalInterface"), e.getMessage());
    }

    @Test
    void onlyAnInterfaceCanBeRegistered() {
        ELMethodRegistry registry = new ELMethodRegistry();

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> registry.functionalInterface(String.class, (context, lambda) -> "no"));
        assertTrue(e.getMessage().contains("interface"), e.getMessage());
    }
}
