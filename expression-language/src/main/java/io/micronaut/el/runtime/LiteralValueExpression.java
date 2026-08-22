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

/**
 * A literal-expression as described in the section 1.2.2 of the Jakarta Expression Language specification.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class LiteralValueExpression extends CompiledValueExpression {

    private static final long serialVersionUID = 1L;

    /**
     * @param text         The literal text
     * @param expectedType The expected type of the evaluation result
     */
    public LiteralValueExpression(String text, Class<?> expectedType) {
        super(text, expectedType);
    }

    @Override
    protected Object evaluate(ELContext context) {
        return getExpressionString();
    }

    @Override
    public Class<?> getType(ELContext context) {
        return String.class;
    }

    @Override
    public boolean isLiteralText() {
        return true;
    }
}
