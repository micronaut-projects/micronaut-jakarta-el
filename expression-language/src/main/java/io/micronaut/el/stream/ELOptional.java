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

/**
 * The {@code Optional} implementation class described in the section 2.3.3.2 of the Jakarta Expression
 * Language specification.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELOptional {

    private static final ELOptional EMPTY = new ELOptional(null);

    private final @Nullable Object value;

    private ELOptional(@Nullable Object value) {
        this.value = value;
    }

    /**
     * @return An empty optional
     */
    public static ELOptional empty() {
        return EMPTY;
    }

    /**
     * @param value The value, can be {@code null}
     * @return An optional holding the value
     */
    public static ELOptional of(@Nullable Object value) {
        return value == null ? EMPTY : new ELOptional(value);
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
    public Object get() {
        if (value == null) {
            throw new ELException("The optional is empty");
        }
        return value;
    }

    /**
     * @param consumer The consumer invoked with the value when it is present
     */
    public void ifPresent(LambdaExpression consumer) {
        if (value != null) {
            consumer.invoke(value);
        }
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
        return value == null ? other.invoke() : value;
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
