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
package io.micronaut.el.interpreter.parity;

import io.micronaut.core.annotation.Introspected;

/**
 * The interpreted twin of the bean of the test suite. The expressions construct it as {@code Constructed(1)}, so
 * its simple name cannot carry the {@code Parity} prefix.
 */
@Introspected
public class Constructed {

    private final String selected;

    public Constructed(Number value) {
        this.selected = "number";
    }

    public Constructed(Object value) {
        this.selected = "object";
    }

    public String getSelected() {
        return selected;
    }
}
