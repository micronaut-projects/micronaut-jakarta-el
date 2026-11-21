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

/**
 * Overloads a primitive and its wrapper tell apart. Kept identical to
 * {@code io.micronaut.el.test.PrimitiveVariableBean}.
 */
public class PrimitiveVariableBean {

    public int getCount() {
        return 2;
    }

    public String which(int value) {
        return "int:" + value;
    }

    public String which(Integer value) {
        return "Integer:" + value;
    }

    public String which(long value) {
        return "long:" + value;
    }

    public String which(Long value) {
        return "Long:" + value;
    }

    public String repeat(int times) {
        return "ab".repeat(times);
    }
}
