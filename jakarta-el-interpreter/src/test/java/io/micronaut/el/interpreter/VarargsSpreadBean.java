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

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The instance methods with a variable arity parameter that an array argument is spread into, section 1.2.1.2 of
 * the specification and section 15.12.2 of the JLS. Kept identical to
 * {@code io.micronaut.el.test.VarargsSpreadBean}.
 */
public class VarargsSpreadBean {

    public String argumentType(Object... values) {
        return values.length + ":" + values[0].getClass().getTypeName();
    }

    public String join(String separator, Object... parts) {
        return Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining(separator));
    }

    public static String staticCount(Object... values) {
        return values.length + ":" + values[0].getClass().getTypeName();
    }
}
