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

import java.util.List;

/**
 * A method expression declared with {@code io.micronaut.el.annotation.ELMethodExpression}.
 *
 * @param expression   The expression string
 * @param returnType   The expected return type
 * @param parameterTypes The expected parameter types
 * @param constantName The name of the generated constant
 * @param node         The parsed expression
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public record ELMethodExpressionDefinition(String expression,
                                           ClassElement returnType,
                                           List<ClassElement> parameterTypes,
                                           String constantName,
                                           ELNode node) {
}
