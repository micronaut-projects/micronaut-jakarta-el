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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link VariableMapper} keeping the variables of the section 1.19 of the Jakarta Expression Language
 * specification in a map.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public final class MapVariableMapper extends VariableMapper {

    /**
     * The variables, an unmodifiable map replaced as a whole rather than modified by {@link #setVariable}: a
     * {@link #snapshot()} keeps the map it was taken from, which no later binding changes, and a reader never sees a
     * map being modified.
     */
    private final AtomicReference<Map<String, ValueExpression>> variables = new AtomicReference<>(Map.of());

    private Map<String, ValueExpression> variables() {
        return Objects.requireNonNull(variables.get());
    }

    /**
     * @return Whether the mapper holds no variable
     */
    public boolean isEmpty() {
        return variables().isEmpty();
    }

    @Override
    @Nullable
    public ValueExpression resolveVariable(String variable) {
        return variables().get(variable);
    }

    @Override
    @Nullable
    public synchronized ValueExpression setVariable(String variable, @Nullable ValueExpression expression) {
        Map<String, ValueExpression> copy = new HashMap<>(variables());
        ValueExpression previous = expression == null ? copy.remove(variable) : copy.put(variable, expression);
        variables.set(Collections.unmodifiableMap(copy));
        return previous;
    }

    /**
     * The variables this mapper holds now, which the bindings it is given later do not change: what a lambda
     * expression closes over when it is created, section 1.20 of the specification.
     *
     * @return A read-only mapper of the current variables
     */
    public VariableMapper snapshot() {
        return new Snapshot(variables());
    }

    /**
     * Whether a snapshot taken of this mapper still holds what the mapper holds.
     *
     * @param snapshot The snapshot
     * @return Whether no variable was bound since the snapshot was taken
     */
    boolean isCurrent(VariableMapper snapshot) {
        return snapshot instanceof Snapshot taken && taken.variables == variables();
    }

    /**
     * The variables a mapper held when the snapshot was taken.
     */
    private static final class Snapshot extends VariableMapper {

        private final Map<String, ValueExpression> variables;

        private Snapshot(Map<String, ValueExpression> variables) {
            this.variables = variables;
        }

        @Override
        @Nullable
        public ValueExpression resolveVariable(String variable) {
            return variables.get(variable);
        }

        @Override
        public ValueExpression setVariable(String variable, @Nullable ValueExpression expression) {
            throw new UnsupportedOperationException("The variables a lambda expression closed over are read-only");
        }
    }
}
