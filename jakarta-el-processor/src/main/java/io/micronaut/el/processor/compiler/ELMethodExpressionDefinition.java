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

import java.util.List;

/**
 * A method expression declared with {@code io.micronaut.el.annotation.ELMethodExpression}.
 *
 * @param expression   The expression string
 * @param returnType   The expected return type
 * @param inferred Whether the type was inferred from the static type of the expression rather than declared
 * @param parameterTypes The expected parameter types
 * @param constantName The name of the generated constant
 * @param node         The parsed expression
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public record ELMethodExpressionDefinition(String expression,
                                           @Nullable ClassElement returnType,
                                           boolean inferred,
                                           List<ClassElement> parameterTypes,
                                           String constantName,
                                           ELNode node) {

    /**
     * A definition whose expected return type was omitted, resolved to the static type of the invocation.
     *
     * @param type The inferred type
     * @return The definition with the type
     */
    public ELMethodExpressionDefinition inferring(ClassElement type) {
        return new ELMethodExpressionDefinition(expression, type, true, parameterTypes, constantName, node);
    }

    /**
     * @return The return type, which inference has resolved by the time a writer reads it
     */
    public ClassElement requireReturnType() {
        return Objects.requireNonNull(returnType, "The return type has not been inferred yet");
    }
}
