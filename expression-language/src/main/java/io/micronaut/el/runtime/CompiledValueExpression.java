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

import org.jspecify.annotations.Nullable;
import jakarta.el.ELContext;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.ValueExpression;

import java.util.Objects;

/**
 * The base class of the {@link ValueExpression} implementations generated at compilation time.
 *
 * <p>Subclasses implement {@link #evaluate(ELContext)}, which contains the compiled form of the
 * expression. Expressions that are lvalues additionally override {@link #setValue(ELContext, Object)},
 * {@link #getType(ELContext)} and {@link #isReadOnly(ELContext)}.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public abstract class CompiledValueExpression extends ValueExpression implements CompiledExpression {

    private static final long serialVersionUID = 1L;

    private final String expressionString;
    private final Class<?> expectedType;

    /**
     * @param expressionString The original expression
     * @param expectedType     The expected type of the evaluation result
     */
    protected CompiledValueExpression(String expressionString, Class<?> expectedType) {
        this.expressionString = Objects.requireNonNull(expressionString, "expressionString");
        this.expectedType = Objects.requireNonNull(expectedType, "expectedType");
    }

    /**
     * Evaluates the compiled expression.
     *
     * @param context The context
     * @return The result of the evaluation, before the coercion to the expected type
     */
    @Nullable
    protected abstract Object evaluate(ELContext context);

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getValue(ELContext context) {
        context.notifyBeforeEvaluation(expressionString);
        Object value = evaluate(context);
        context.notifyAfterEvaluation(expressionString);
        return (T) ELSupport.coerceToType(context, value, expectedType);
    }

    @Override
    public void setValue(ELContext context, @Nullable Object value) {
        throw new PropertyNotWritableException("The expression '" + expressionString + "' is not an lvalue");
    }

    @Override
    public boolean isReadOnly(ELContext context) {
        return true;
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext context) {
        return null;
    }

    @Override
    public Class<?> getExpectedType() {
        return expectedType;
    }

    @Override
    public final String getExpressionString() {
        return expressionString;
    }

    @Override
    public boolean isLiteralText() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CompiledValueExpression other
            && other.getClass() == getClass()
            && other.expressionString.equals(expressionString)
            && other.expectedType.equals(expectedType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), expressionString, expectedType);
    }

    @Override
    public String toString() {
        return "ValueExpression[" + expressionString + "]";
    }
}
