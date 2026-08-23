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

import io.micronaut.el.runtime.ObjectValueExpression;
import jakarta.el.ValueExpression;

import java.util.List;

/**
 * A source whose service descriptor is only reachable through the child class loader of
 * {@link CompiledExpressionFactoryTest}: the class is on the test classpath, the descriptor under
 * {@code child-loader/META-INF/micronaut/...} is not.
 */
public final class ChildLoaderExpressionSource implements ELExpressionSource {

    public static final String EXPRESSION = "${fromTheChildLoader}";

    @Override
    public List<String> expressions() {
        return List.of(EXPRESSION);
    }

    @Override
    public ValueExpression createValueExpression(String expression, Class<?> expectedType) {
        return EXPRESSION.equals(expression) ? new ObjectValueExpression("child", expectedType) : null;
    }
}
