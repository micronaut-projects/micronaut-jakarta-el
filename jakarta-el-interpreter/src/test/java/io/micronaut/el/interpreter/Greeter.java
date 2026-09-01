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
 * A plain type: no bean introspection, no annotation, nothing the built-in executors know about, so an
 * expression can only reach its methods through a contributed executor or through reflection.
 */
public final class Greeter {

    private final String name;

    public Greeter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String greet(String whom) {
        return "hello " + whom + ", " + name;
    }

    public String greet(Integer times) {
        return "hello ".repeat(times);
    }

    public int count() {
        return name.length();
    }

    public String join(String separator, String... parts) {
        return String.join(separator, parts);
    }
}
