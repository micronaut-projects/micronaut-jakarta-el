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
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.ELExpressionParser;
import io.micronaut.el.parser.ELParser;
import io.micronaut.el.parser.ELIdentifiers;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.el.runtime.ELVariableBindings;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;

import java.util.LinkedHashMap;
import java.util.List;
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

    private final List<ELMethodExecutor> executors;

    private final Map<String, Parsed> parsed = new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Parsed> eldest) {
            return size() > CACHE_SIZE;
        }
    };

    /**
     * Creates a parser using the method executors visible to the context class loader.
     */
    public InterpretingELExpressionParser() {
        this(loadExecutors());
    }

    /**
     * Creates a parser using the given method executors, instead of the ones the context class loader
     * declares as services.
     *
     * <p>This is how an application registers its own executor programmatically, and the only way it can
     * leave one out: a deployment that must not reach a method reflectively passes the executors it wants,
     * rather than relying on the reflective one being absent from the classpath.</p>
     *
     * @param executors The executors, in any order; they are consulted following the {@link
     *                  io.micronaut.core.order.Ordered} contract
     */
    public InterpretingELExpressionParser(List<ELMethodExecutor> executors) {
        this.executors = ELInterpreter.orderExecutors(executors);
    }

    @Override
    public ValueExpression createValueExpression(@Nullable ELContext context,
                                                 String expression,
                                                 Class<?> expectedType) {
        Parsed entry = parse(expression);
        Map<String, ELMethod> functions = ELInterpreter.bindFunctions(context, entry.node(), executors);
        ELInterpreter interpreter = ELInterpreter.sharing(entry.root(), executors, functions);
        ValueExpression interpreted = new InterpretedValueExpression(expression, expectedType, entry.node(),
            functions, interpreter);
        return ELVariableBindings.bind(context, interpreted,
            ELIdentifiers.free(entry.node()).toArray(String[]::new));
    }

    @Override
    public MethodExpression createMethodExpression(@Nullable ELContext context,
                                                   String expression,
                                                   Class<?> expectedReturnType,
                                                   Class<?> @Nullable [] expectedParamTypes) {
        Parsed entry = parse(expression);
        ELNode node = entry.node();
        if (node instanceof ELNode.Composite) {
            throw new ELException("A method expression must consist of a single eval-expression: " + expression);
        }
        requireMethodReference(expression, node);
        Map<String, ELMethod> functions = ELInterpreter.bindFunctions(context, node, executors);
        MethodExpression interpreted = new InterpretedMethodExpression(expression, expectedReturnType,
            expectedParamTypes, node, functions, ELInterpreter.sharing(entry.root(), executors, functions));
        return ELVariableBindings.bind(context, interpreted, ELIdentifiers.free(node).toArray(String[]::new));
    }

    private synchronized Parsed parse(String expression) {
        Parsed entry = parsed.get(expression);
        if (entry == null) {
            ELNode node = ELParser.parse(expression);
            // The evaluator tree contains no context-bound state. Functions and executors are supplied by the
            // expression that runs it, so every expression created from this string shares the same tree.
            entry = new Parsed(node, ELInterpreter.of(executors, Map.of()).compile(node));
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

    synchronized @Nullable Object cachedEvaluator(String expression) {
        Parsed entry = parsed.get(expression);
        return entry == null ? null : entry.root();
    }

    private static List<ELMethodExecutor> loadExecutors() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = InterpretingELExpressionParser.class.getClassLoader();
        }
        return SoftServiceLoader.load(ELMethodExecutor.class, classLoader).collectAll();
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
     * @param root The evaluators compiled from it
     */
    private record Parsed(ELNode node, ELInterpreter.Evaluator root) {
    }
}
