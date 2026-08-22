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
package io.micronaut.el.interpreter;

import io.micronaut.core.annotation.Internal;
import io.micronaut.el.ELExpressionParser;
import io.micronaut.el.parser.ELParser;
import io.micronaut.el.parser.ast.ELNode;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import org.jspecify.annotations.Nullable;

/**
 * Creates the expressions that were not compiled at compilation time by parsing them at runtime.
 *
 * <p>The implementation is registered as a service, so adding this module to the runtime classpath is enough
 * to make {@code jakarta.el.ExpressionFactory} accept expression strings that are only known at runtime.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class InterpretingELExpressionParser implements ELExpressionParser {

    @Override
    public ValueExpression createValueExpression(@Nullable ELContext context,
                                                 String expression,
                                                 Class<?> expectedType) {
        ELNode node = ELParser.parse(expression);
        return new InterpretedValueExpression(expression, expectedType, node, ELInterpreter.of(context, node));
    }

    @Override
    public MethodExpression createMethodExpression(@Nullable ELContext context,
                                                   String expression,
                                                   Class<?> expectedReturnType,
                                                   Class<?>[] expectedParamTypes) {
        ELNode node = ELParser.parse(expression);
        if (node instanceof ELNode.Composite) {
            throw new ELException("A method expression must consist of a single eval-expression: " + expression);
        }
        requireMethodReference(expression, node);
        return new InterpretedMethodExpression(expression, expectedReturnType, expectedParamTypes, node,
            ELInterpreter.of(context, node));
    }

    /**
     * A method expression references a method, so an eval-expression computing a value cannot be one.
     */
    private static void requireMethodReference(String expression, ELNode node) {
        ELNode unwrapped = node instanceof ELNode.Eval eval ? eval.expression() : node;
        if (unwrapped instanceof ELNode.Identifier
            || unwrapped instanceof ELNode.Property
            || unwrapped instanceof ELNode.Method) {
            return;
        }
        throw new ELException("The expression '" + expression
            + "' is not a method expression, it does not reference a method");
    }
}
