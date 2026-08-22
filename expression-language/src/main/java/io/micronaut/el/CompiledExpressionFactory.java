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
package io.micronaut.el;

import org.jspecify.annotations.Nullable;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.el.resolver.StreamELResolver;
import io.micronaut.el.runtime.ELLiterals;
import io.micronaut.el.runtime.ELSupport;
import io.micronaut.el.runtime.LiteralMethodExpression;
import io.micronaut.el.runtime.LiteralValueExpression;
import io.micronaut.el.runtime.ObjectValueExpression;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The {@link ExpressionFactory} returning the expressions compiled at compilation time.
 *
 * <p>The factory does not parse expressions at runtime. An expression that was not compiled, and that is
 * not a literal-expression, cannot be created.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public class CompiledExpressionFactory extends ExpressionFactory {

    /**
     * The sources declaring each expression string, in their load order.
     */
    private final Map<String, List<ELExpressionSource>> indexed;
    /**
     * The sources declaring no expression strings, consulted for every expression.
     */
    private final List<ELExpressionSource> unindexed;
    private final @Nullable ELExpressionParser parser;

    /**
     * Creates a factory loading the generated expression sources with the {@link SoftServiceLoader}.
     *
     * <p>The sources are loaded with the context class loader of the thread, which is the loader
     * {@link ExpressionFactory#newInstance()} locates this factory with, so that the expressions generated
     * into a class loader that is a child of the one holding this class — a deployment, a plugin, an isolated
     * test — are found. The loader of this class is used when the thread has none.</p>
     */
    public CompiledExpressionFactory() {
        this(contextClassLoader());
    }

    /**
     * Creates a factory loading the generated expression sources from the given class loader.
     *
     * @param classLoader The class loader holding the generated sources and the optional parser
     */
    public CompiledExpressionFactory(ClassLoader classLoader) {
        this(
            SoftServiceLoader.load(ELExpressionSource.class, classLoader).collectAll(),
            SoftServiceLoader.load(ELExpressionParser.class, classLoader).firstAvailable().orElse(null)
        );
    }

    /**
     * @param sources The expression sources
     */
    public CompiledExpressionFactory(List<ELExpressionSource> sources) {
        this(sources, null);
    }

    /**
     * @param sources The expression sources
     * @param parser  The parser creating the expressions that were not compiled, can be {@code null}
     */
    public CompiledExpressionFactory(List<ELExpressionSource> sources, @Nullable ELExpressionParser parser) {
        Map<String, List<ELExpressionSource>> indexed = new HashMap<>();
        List<ELExpressionSource> unindexed = new ArrayList<>();
        for (ELExpressionSource source : sources) {
            List<String> expressions = source.expressions();
            if (expressions.isEmpty()) {
                unindexed.add(source);
                continue;
            }
            for (String expression : expressions) {
                indexed.computeIfAbsent(expression, key -> new ArrayList<>(1)).add(source);
            }
        }
        this.indexed = indexed;
        this.unindexed = unindexed;
        this.parser = parser;
    }

    private static ClassLoader contextClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader == null ? CompiledExpressionFactory.class.getClassLoader() : classLoader;
    }

    /**
     * The sources that may declare the expression: the ones indexed under its string, then the ones that
     * declare no strings. A source generated before {@link ELExpressionSource#expressions()} existed falls in
     * the second group and keeps working.
     */
    private List<ELExpressionSource> sourcesOf(@Nullable String expression) {
        List<ELExpressionSource> declaring = expression == null ? null : indexed.get(expression);
        if (declaring == null) {
            return unindexed;
        }
        if (unindexed.isEmpty()) {
            return declaring;
        }
        List<ELExpressionSource> all = new ArrayList<>(declaring.size() + unindexed.size());
        all.addAll(declaring);
        all.addAll(unindexed);
        return all;
    }

    @Override
    public ValueExpression createValueExpression(ELContext context, String expression, Class<?> expectedType) {
        Class<?> type = Objects.requireNonNull(expectedType, "The expected type cannot be null");
        for (ELExpressionSource source : sourcesOf(expression)) {
            ValueExpression valueExpression = source.createValueExpression(expression, type);
            if (valueExpression != null) {
                return valueExpression;
            }
        }
        String literalText = expression == null ? null : ELLiterals.literalTextOrNull(expression);
        if (literalText != null) {
            return new LiteralValueExpression(literalText, type);
        }
        if (parser != null) {
            return parser.createValueExpression(context, expression, type);
        }
        throw notCompiled(expression);
    }

    @Override
    public ValueExpression createValueExpression(Object instance, Class<?> expectedType) {
        return new ObjectValueExpression(instance, Objects.requireNonNull(expectedType, "The expected type cannot be null"));
    }

    @Override
    public MethodExpression createMethodExpression(ELContext context,
                                                   String expression,
                                                   Class<?> expectedReturnType,
                                                   Class<?>[] expectedParamTypes) {
        Class<?> returnType = expectedReturnType == null ? Object.class : expectedReturnType;
        Class<?>[] paramTypes = expectedParamTypes;
        for (ELExpressionSource source : sourcesOf(expression)) {
            MethodExpression methodExpression = source.createMethodExpression(expression, returnType, paramTypes);
            if (methodExpression != null) {
                return requireParamTypes(methodExpression, paramTypes);
            }
        }
        String literalText = expression == null ? null : ELLiterals.literalTextOrNull(expression);
        if (literalText != null) {
            return requireParamTypes(new LiteralMethodExpression(literalText, returnType, paramTypes), paramTypes);
        }
        if (parser != null) {
            return requireParamTypes(parser.createMethodExpression(context, expression, returnType, paramTypes),
                paramTypes);
        }
        throw notCompiled(expression);
    }

    @Override
    @Nullable
    public <T> T coerceToType(@Nullable Object obj, @Nullable Class<T> targetType) {
        return ELSupport.coerceToFunctionalInterface(null, obj, targetType);
    }

    @Override
    public ELResolver getStreamELResolver() {
        return new StreamELResolver();
    }

    @Override
    public Map<String, Method> getInitFunctionMap() {
        return Map.of();
    }

    /**
     * The parameter types can only be omitted when the expression provides its own parameters.
     */
    private static MethodExpression requireParamTypes(MethodExpression methodExpression,
                                                      @Nullable Class<?>[] paramTypes) {
        if (paramTypes == null && !methodExpression.isParametersProvided()) {
            throw new NullPointerException("The expected parameter types are required for the method expression '"
                + methodExpression.getExpressionString() + "', which does not provide its own parameters");
        }
        return methodExpression;
    }

    private static ELException notCompiled(String expression) {
        return new ELException("The expression '" + expression
            + "' was not compiled. Declare it with @ELExpression so that it is compiled at compilation time, or add"
            + " the micronaut-expression-language-interpreter module to parse it at runtime.");
    }
}
