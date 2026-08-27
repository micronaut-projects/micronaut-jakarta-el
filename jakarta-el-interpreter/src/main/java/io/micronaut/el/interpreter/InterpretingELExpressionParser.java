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

import java.util.LinkedHashMap;
import java.util.Map;
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

    /**
     * The number of parsed expressions kept: the syntax trees are immutable and an application evaluates the
     * same strings repeatedly, so a bounded cache saves the parse, as the other implementations do.
     */
    static final int CACHE_SIZE = 2048;

    private final Map<String, Parsed> parsed = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Parsed> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    @Override
    public ValueExpression createValueExpression(@Nullable ELContext context,
                                                 String expression,
                                                 Class<?> expectedType) {
        Parsed entry = parse(expression);
        ELInterpreter interpreter = entry.root() == null
            ? ELInterpreter.of(context, entry.node())
            : ELInterpreter.sharing(entry.root());
        return new InterpretedValueExpression(expression, expectedType, entry.node(), interpreter);
    }

    @Override
    public MethodExpression createMethodExpression(@Nullable ELContext context,
                                                   String expression,
                                                   Class<?> expectedReturnType,
                                                   Class<?> @Nullable [] expectedParamTypes) {
        ELNode node = parse(expression).node();
        if (node instanceof ELNode.Composite) {
            throw new ELException("A method expression must consist of a single eval-expression: " + expression);
        }
        requireMethodReference(expression, node);
        return new InterpretedMethodExpression(expression, expectedReturnType, expectedParamTypes, node,
            ELInterpreter.of(context, node));
    }

    private synchronized Parsed parse(String expression) {
        Parsed entry = parsed.get(expression);
        if (entry == null) {
            ELNode node = ELParser.parse(expression);
            // an expression without functions evaluates the same way under every context, so its evaluators
            // are compiled once and shared by the expressions created from the string
            entry = new Parsed(node, ELInterpreter.containsFunction(node) ? null : ELInterpreter.of(null, node).compile(node));
            parsed.put(expression, entry);
        }
        return entry;
    }

    synchronized int cachedExpressions() {
        return parsed.size();
    }

    synchronized boolean isCached(String expression) {
        return parsed.containsKey(expression);
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

    /**
     * A parsed expression, with its evaluators when they can be shared.
     *
     * @param node The syntax tree
     * @param root The evaluators compiled from it, {@code null} when the expression binds functions
     */
    private record Parsed(ELNode node, ELInterpreter.@Nullable Evaluator root) {
    }
}
