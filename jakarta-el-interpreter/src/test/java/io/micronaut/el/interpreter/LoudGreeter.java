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
 * A subtype of {@link Greeter}: it inherits the methods registered for its supertype, but not the constructor,
 * which would otherwise construct a {@link Greeter} where a {@code LoudGreeter} was asked for.
 */
public class LoudGreeter extends Greeter {

    public LoudGreeter(String name) {
        super(name);
    }

    @Override
    public String greet(String whom) {
        return super.greet(whom).toUpperCase();
    }
}
