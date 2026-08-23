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
package io.micronaut.el.processor.compiler;

import io.micronaut.core.annotation.Internal;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.inject.ast.ClassElement;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A value expression declared with {@code io.micronaut.el.annotation.ELExpression}.
 *
 * @param expression   The expression string
 * @param expectedType The expected type of the evaluation result
 * @param inferred Whether the type was inferred from the static type of the expression rather than declared
 * @param constantName The name of the generated constant
 * @param node         The parsed expression
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public record ELExpressionDefinition(String expression,
                                     @Nullable ClassElement expectedType,
                                     boolean inferred,
                                     String constantName,
                                     ELNode node) {

    /**
     * A definition whose expected type was omitted, resolved to the static type of the expression.
     *
     * @param type The inferred type
     * @return The definition with the type
     */
    public ELExpressionDefinition inferring(ClassElement type) {
        return new ELExpressionDefinition(expression, type, true, constantName, node);
    }

    /**
     * @return The expected type, which inference has resolved by the time a writer reads it
     */
    public ClassElement requireExpectedType() {
        return Objects.requireNonNull(expectedType, "The expected type has not been inferred yet");
    }
}
