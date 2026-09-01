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

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.order.Ordered;

/**
 * Contributes the methods, constructors and functions that expressions parsed at runtime may call, without
 * reflection.
 *
 * <p>This is the service an application implements to make a type callable from a runtime-parsed expression.
 * Where {@link ELMethodExecutor} decides at every call which method a name refers to — which is what a
 * reflective or otherwise dynamic provider needs — a contributor declares its callable surface once, at
 * startup, and the registry does the rest: it selects the overload as the section 1.6 of the specification
 * requires, coerces the arguments to the declared parameter types, packs the variable arity ones, reports the
 * metadata a {@code jakarta.el.MethodExpression} exposes, and builds an identity that compares equal to the
 * same expression compiled at build time.</p>
 *
 * <p>Implementations are discovered as services, so a contributor is a class named in
 * {@code META-INF/services/io.micronaut.el.ELMethodContributor}:</p>
 *
 * <pre>{@code
 * public final class BookMethods implements ELMethodContributor {
 *
 *     @Override
 *     public void contribute(ELMethodRegistry registry) {
 *         registry.method(Book.class, "discounted", double.class, Integer.class, Book::discounted)
 *                 .staticMethod(Math.class, "abs", int.class, int.class, Math::abs)
 *                 .constructor(Book.class, Book::new, String.class)
 *                 .function("fmt", "join", Formatting.class, "join", String.class, Formatting::join,
 *                           String.class, String[].class);
 *     }
 * }
 * }</pre>
 *
 * <p>Contributions are collected once, when the expression parser is created, and indexed by type and name.
 * A contributor therefore cannot see the {@code jakarta.el.ELContext}: anything that depends on it — a
 * {@code jakarta.el.FunctionMapper} lookup, a per-request bean — belongs on the {@link ELMethodExecutor}
 * contract instead. The receiver of an instance method is not affected, since it reaches the invocation at
 * evaluation time.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.1
 * @see ELMethodRegistry
 * @see ELMethodExecutor
 */
@Experimental
@FunctionalInterface
public interface ELMethodContributor extends Ordered {

    /**
     * Registers the methods, constructors and functions this contributor makes callable.
     *
     * <p>Called once, when the expression parser is created. Registering the same signature twice makes the
     * reference to it ambiguous, exactly as two equally specific overloads would.</p>
     *
     * @param registry The registry to declare them in
     */
    void contribute(ELMethodRegistry registry);

    /**
     * Returns the order in which contributors are consulted. Lower values run first, following the Micronaut
     * {@link Ordered} contract; registrations of different contributors are merged, so the order only decides
     * which of two identical signatures is declared first.
     *
     * @return The order, zero by default
     */
    @Override
    default int getOrder() {
        return 0;
    }
}
