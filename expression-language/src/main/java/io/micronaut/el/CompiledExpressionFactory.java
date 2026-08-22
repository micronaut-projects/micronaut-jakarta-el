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
import java.util.Objects;
import java.util.List;
import java.util.Map;

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

    private final List<ELExpressionSource> sources;
    private final @Nullable ELExpressionParser parser;

    /**
     * Creates a factory loading the generated expression sources with the {@link SoftServiceLoader}.
     */
    public CompiledExpressionFactory() {
        this(
            SoftServiceLoader.load(ELExpressionSource.class, CompiledExpressionFactory.class.getClassLoader())
                .collectAll(),
            SoftServiceLoader.load(ELExpressionParser.class, CompiledExpressionFactory.class.getClassLoader())
                .firstAvailable()
                .orElse(null)
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
        this.sources = sources;
        this.parser = parser;
    }

    @Override
    public ValueExpression createValueExpression(ELContext context, String expression, Class<?> expectedType) {
        Class<?> type = Objects.requireNonNull(expectedType, "The expected type cannot be null");
        for (ELExpressionSource source : sources) {
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
        for (ELExpressionSource source : sources) {
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
