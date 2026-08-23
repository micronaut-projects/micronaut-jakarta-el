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
package io.micronaut.el.runtime;

import io.micronaut.core.annotation.Experimental;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link VariableMapper} keeping the variables of the section 1.19 of the Jakarta Expression Language
 * specification in a map.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public final class MapVariableMapper extends VariableMapper {

    private final Map<String, ValueExpression> variables = new HashMap<>();

    /**
     * @return Whether the mapper holds no variable
     */
    public boolean isEmpty() {
        return variables.isEmpty();
    }

    @Override
    @Nullable
    public ValueExpression resolveVariable(String variable) {
        return variables.get(variable);
    }

    @Override
    @Nullable
    public ValueExpression setVariable(String variable, @Nullable ValueExpression expression) {
        if (expression == null) {
            return variables.remove(variable);
        }
        return variables.put(variable, expression);
    }
}
