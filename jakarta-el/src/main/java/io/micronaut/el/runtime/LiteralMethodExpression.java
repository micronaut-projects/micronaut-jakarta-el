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
import jakarta.el.ELException;
import jakarta.el.MethodInfo;
import jakarta.el.MethodReference;

/**
 * A literal-expression used as a method expression, as described in the section 1.2.2 of the Jakarta
 * Expression Language specification.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class LiteralMethodExpression extends CompiledMethodExpression {

    private static final long serialVersionUID = 1L;

    /**
     * @param text               The literal text
     * @param expectedReturnType The expected return type
     * @param expectedParamTypes The expected parameter types
     */
    public LiteralMethodExpression(String text,
                                   Class<?> expectedReturnType,
                                   Class<?> @Nullable [] expectedParamTypes) {
        super(text, expectedReturnType, expectedParamTypes, false);
    }

    @Override
    @Nullable
    protected Object evaluateBase(ELContext context) {
        return null;
    }

    @Override
    @Nullable
    protected Object evaluateProperty(ELContext context) {
        return null;
    }

    @Override
    protected Object doInvoke(ELContext context, Object @Nullable [] arguments) {
        if (getExpectedReturnType() == void.class) {
            throw new ELException("A literal-expression cannot be used as a method expression returning void");
        }
        return getExpressionString();
    }

    @Override
    public MethodInfo getMethodInfo(ELContext context) {
        return new MethodInfo(getExpressionString(), getExpectedReturnType(), getExpectedParamTypes());
    }

    @Override
    @SuppressWarnings("NullAway")
    public MethodReference getMethodReference(ELContext context) {
        context.notifyBeforeEvaluation(getExpressionString());
        context.notifyAfterEvaluation(getExpressionString());
        return null;
    }

    @Override
    public boolean isLiteralText() {
        return true;
    }
}
