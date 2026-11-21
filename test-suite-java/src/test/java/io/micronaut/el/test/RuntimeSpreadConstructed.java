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
package io.micronaut.el.test;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A class constructed with a variable arity {@code Object...} parameter, imported by
 * {@link RuntimeVarargsSpreadExpressions}.
 */
public final class RuntimeSpreadConstructed {

    private final String value;

    public RuntimeSpreadConstructed(Object... values) {
        value = values.length + ":" + Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(","));
    }

    public String getValue() {
        return value;
    }
}
