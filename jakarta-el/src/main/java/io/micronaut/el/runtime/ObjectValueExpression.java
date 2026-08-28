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

import org.jspecify.annotations.Nullable;
import jakarta.el.ELContext;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.ValueExpression;

import java.util.Objects;

/**
 * A value expression wrapping an already resolved value, as created by
 * {@code ExpressionFactory.createValueExpression(Object, Class)}.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ObjectValueExpression extends ValueExpression {

    private static final long serialVersionUID = 1L;

    private final @Nullable Object value;
    private final Class<?> expectedType;

    /**
     * @param value        The value
     * @param expectedType The expected type
     */
    public ObjectValueExpression(@Nullable Object value, Class<?> expectedType) {
        this.value = value;
        this.expectedType = Objects.requireNonNull(expectedType, "expectedType");
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getValue(ELContext context) {
        return (T) ELSupport.coerceToType(context, value, expectedType);
    }

    @Override
    public void setValue(ELContext context, @Nullable Object value) {
        throw new PropertyNotWritableException("The value expression is not an lvalue");
    }

    @Override
    public boolean isReadOnly(ELContext context) {
        return true;
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext context) {
        return value == null ? null : value.getClass();
    }

    @Override
    public Class<?> getExpectedType() {
        return expectedType;
    }

    @Override
    @Nullable
    public String getExpressionString() {
        return value == null ? null : value.toString();
    }

    @Override
    public boolean isLiteralText() {
        return true;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ObjectValueExpression other
            && Objects.equals(other.value, value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }
}
