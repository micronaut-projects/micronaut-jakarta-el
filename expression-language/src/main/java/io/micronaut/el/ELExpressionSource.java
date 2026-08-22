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
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;

import java.util.List;

/**
 * A source of the expressions compiled at compilation time.
 *
 * <p>An implementation is generated for every type declaring expressions with
 * {@code io.micronaut.el.annotation.ELExpression} and is registered as a service, so that
 * {@link CompiledExpressionFactory} can return the compiled expression instead of parsing the expression
 * string.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public interface ELExpressionSource {

    /**
     * The expression strings this source declares, so that {@link CompiledExpressionFactory} can index the
     * sources once and consult only the ones declaring the expression it is asked for.
     *
     * <p>A source returning an empty list is consulted for every expression, which keeps a source generated
     * before this method existed working, at the cost of a lookup per expression.</p>
     *
     * @return The expression strings, value and method expressions alike
     */
    default List<String> expressions() {
        return List.of();
    }

    /**
     * @param expression   The expression string
     * @param expectedType The expected type
     * @return The compiled value expression or {@code null} when this source does not declare it
     */
    @Nullable
    default ValueExpression createValueExpression(String expression, Class<?> expectedType) {
        return null;
    }

    /**
     * @param expression         The expression string
     * @param expectedReturnType The expected return type
     * @param expectedParamTypes The expected parameter types
     * @return The compiled method expression or {@code null} when this source does not declare it
     */
    @Nullable
    default MethodExpression createMethodExpression(String expression,
                                                    Class<?> expectedReturnType,
                                                    Class<?>[] expectedParamTypes) {
        return null;
    }
}
