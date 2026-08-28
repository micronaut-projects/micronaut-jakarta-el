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
package io.micronaut.el.stream;

import io.micronaut.core.annotation.Internal;

import org.jspecify.annotations.Nullable;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import jakarta.el.MethodNotFoundException;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The {@code Optional} implementation class described in the section 2.3.3.2 of the Jakarta Expression
 * Language specification.
 *
 * @param <T> The type of the value
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELOptional<T> {

    private static final ELOptional<?> EMPTY = new ELOptional<>(null);

    private final @Nullable T value;

    private ELOptional(@Nullable T value) {
        this.value = value;
    }

    /**
     * @param <T> The type of the value
     * @return An empty optional
     */
    @SuppressWarnings("unchecked")
    public static <T> ELOptional<T> empty() {
        return (ELOptional<T>) EMPTY;
    }

    /**
     * @param value The value, can be {@code null}
     * @param <T>   The type of the value
     * @return An optional holding the value
     */
    public static <T> ELOptional<T> of(@Nullable T value) {
        return value == null ? empty() : new ELOptional<>(value);
    }

    /**
     * @return True if a value is present
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * @return The value held by the optional
     */
    public T get() {
        if (value == null) {
            throw new ELException("The optional is empty");
        }
        return value;
    }

    /**
     * @param consumer The consumer invoked with the value when it is present
     * @return Always {@code null}
     */
    @Nullable
    public Object ifPresent(LambdaExpression consumer) {
        return ifPresent(consumer::invoke);
    }

    /**
     * @param consumer The consumer invoked with the value when it is present, as compiled from a lambda expression
     * @return Always {@code null}
     */
    @Nullable
    public Object ifPresent(Consumer<? super T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
        return null;
    }

    /**
     * @param other The value returned when the optional is empty
     * @return The value held by the optional or the given value
     */
    @Nullable
    public Object orElse(@Nullable Object other) {
        return value == null ? other : value;
    }

    /**
     * @param other The lambda expression invoked when the optional is empty
     * @return The value held by the optional or the result of the lambda expression
     */
    @Nullable
    public Object orElseGet(LambdaExpression other) {
        return orElseGet(other::invoke);
    }

    /**
     * @param other The supplier invoked when the optional is empty, as compiled from a lambda expression
     * @return The value held by the optional or the supplied value
     */
    @Nullable
    public Object orElseGet(Supplier<?> other) {
        return value == null ? other.get() : value;
    }

    /**
     * Invokes one of the operations of this class, avoiding the reflective dispatch of the standard
     * resolvers.
     *
     * @param context   The context
     * @param name      The name of the operation
     * @param arguments The arguments
     * @return The result of the operation
     */
    @Nullable
    public Object invokeOperation(ELContext context, String name, Object[] arguments) {
        requireArity(name, arguments.length);
        return switch (name) {
            case "get" -> get();
            case "isPresent" -> isPresent();
            case "ifPresent" -> {
                ifPresent(ELStream.lambda(context, arguments, 0, name));
                yield null;
            }
            case "orElse" -> orElse(ELStream.argument(arguments, 0, name));
            case "orElseGet" -> orElseGet(ELStream.lambda(context, arguments, 0, name));
            default -> throw new MethodNotFoundException("Unknown optional operation '" + name + "'");
        };
    }

    private static void requireArity(String operation, int count) {
        boolean valid = switch (operation) {
            case "get", "isPresent" -> count == 0;
            case "ifPresent", "orElse", "orElseGet" -> count == 1;
            default -> true;
        };
        if (!valid) {
            throw new MethodNotFoundException("The optional operation '" + operation + "' does not accept "
                + count + " argument(s)");
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ELOptional other && Objects.equals(other.value, value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value == null ? "Optional.empty" : "Optional[" + value + "]";
    }
}
