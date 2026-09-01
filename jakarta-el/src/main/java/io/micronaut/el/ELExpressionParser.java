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

import io.micronaut.core.annotation.Experimental;
import jakarta.el.ELContext;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import org.jspecify.annotations.Nullable;

/**
 * Creates the expressions that were not compiled at compilation time.
 *
 * <p>The compiled expressions are the fast path: they are parsed, type checked and turned into Java source
 * when the annotation processor runs. An implementation of this interface is only consulted for the
 * expressions that are not known at compilation time, which is the case when an expression string is built
 * at runtime. When no implementation is on the classpath such an expression cannot be created.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 * @see CompiledExpressionFactory
 */
@Experimental
public interface ELExpressionParser {

    /**
     * Parses a value expression.
     *
     * @param context      The context the expression is created for, can be {@code null}
     * @param expression   The expression
     * @param expectedType The expected type of the evaluation result
     * @return The value expression
     */
    ValueExpression createValueExpression(@Nullable ELContext context, String expression, Class<?> expectedType);

    /**
     * Parses a method expression.
     *
     * @param context            The context the expression is created for, can be {@code null}
     * @param expression         The expression
     * @param expectedReturnType The expected return type
     * @param expectedParamTypes The expected parameter types
     * @return The method expression
     */
    MethodExpression createMethodExpression(@Nullable ELContext context,
                                            String expression,
                                            Class<?> expectedReturnType,
                                            Class<?> @Nullable [] expectedParamTypes);
}
