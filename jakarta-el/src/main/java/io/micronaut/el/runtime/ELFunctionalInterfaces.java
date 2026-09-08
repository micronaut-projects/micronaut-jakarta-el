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
package io.micronaut.el.runtime;

import io.micronaut.core.annotation.Internal;
import jakarta.el.ELContext;
import jakarta.el.LambdaExpression;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Presents a lambda expression as one of the functional interfaces of the platform, without reflection.
 *
 * <p>The coercion of the section 1.23.2 applies to any functional interface, and the single abstract method of
 * one the application declares is only known at runtime, which is what a
 * {@code java.lang.reflect.Proxy} is for. The interfaces below are known here instead: their method, its arity
 * and its return type are part of this module, so the coercion to them is an ordinary implementation and needs
 * no reflection and no registration in a native image.</p>
 *
 * <p>A lambda written in place against a statically known parameter type never reaches this: the compiler
 * implements the interface directly. This is the path of an expression whose method is selected by the
 * resolver chain at evaluation time.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
final class ELFunctionalInterfaces {

    private static final Map<Class<?>, Factory> KNOWN = Map.ofEntries(
        Map.entry(Supplier.class, (context, lambda) -> (Supplier<Object>) () -> call(context, lambda)),
        Map.entry(Callable.class, (context, lambda) -> (Callable<Object>) () -> call(context, lambda)),
        Map.entry(Runnable.class, (context, lambda) -> (Runnable) () -> call(context, lambda)),
        Map.entry(Function.class, (context, lambda) -> (Function<Object, Object>) first -> call(context, lambda, first)),
        Map.entry(UnaryOperator.class, (context, lambda) -> (UnaryOperator<Object>) first -> call(context, lambda, first)),
        Map.entry(BiFunction.class, (context, lambda) ->
            (BiFunction<Object, Object, Object>) (first, second) -> call(context, lambda, first, second)),
        Map.entry(BinaryOperator.class, (context, lambda) ->
            (BinaryOperator<Object>) (first, second) -> call(context, lambda, first, second)),
        Map.entry(Consumer.class, (context, lambda) -> (Consumer<Object>) first -> call(context, lambda, first)),
        Map.entry(BiConsumer.class, (context, lambda) ->
            (BiConsumer<Object, Object>) (first, second) -> call(context, lambda, first, second)),
        Map.entry(Predicate.class, (context, lambda) ->
            (Predicate<Object>) first -> ELSupport.coerceToBoolean(call(context, lambda, first), true)),
        Map.entry(BiPredicate.class, (context, lambda) -> (BiPredicate<Object, Object>) (first, second) ->
            ELSupport.coerceToBoolean(call(context, lambda, first, second), true)),
        // the method returns a primitive, so a lambda handing back null coerces to zero rather than
        // failing to unbox, which is what the proxy does with the return type it reads from the method
        Map.entry(Comparator.class, (context, lambda) -> (Comparator<Object>) (first, second) ->
            ELSupport.coerceToType(context, call(context, lambda, first, second), int.class))
    );

    private ELFunctionalInterfaces() {
    }

    /**
     * Whether the coercion to the given type is implemented here.
     *
     * @param type The target type
     * @return Whether the type is one of the known functional interfaces
     */
    static boolean isKnown(Class<?> type) {
        return KNOWN.containsKey(type);
    }

    /**
     * Presents the lambda expression as an instance of the given known functional interface.
     *
     * @param context The context, or {@code null} when the lambda carries its own
     * @param lambda  The lambda expression
     * @param type    The functional interface
     * @return The instance
     */
    static Object create(@Nullable ELContext context, LambdaExpression lambda, Class<?> type) {
        Factory factory = KNOWN.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Not a known functional interface: " + type.getName());
        }
        return factory.create(context, lambda);
    }

    @Nullable
    private static Object call(@Nullable ELContext context, LambdaExpression lambda, Object... arguments) {
        return context == null ? lambda.invoke(arguments) : lambda.invoke(context, arguments);
    }

    @FunctionalInterface
    private interface Factory {

        Object create(@Nullable ELContext context, LambdaExpression lambda);
    }
}
