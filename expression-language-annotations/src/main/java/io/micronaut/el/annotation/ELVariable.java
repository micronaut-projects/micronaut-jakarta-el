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
package io.micronaut.el.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the compilation time type of an expression variable.
 *
 * <p>A variable declared this way is looked up in the {@code jakarta.el.ELContext} at evaluation time,
 * but every property access, method invocation and coercion applied to it is resolved statically, which
 * removes the reflective {@code jakarta.el.ELResolver} lookups from the generated expression.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 * @see ELEnvironment
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface ELVariable {

    /**
     * @return The name of the variable as it appears in the expression
     */
    String name();

    /**
     * @return The static type of the variable
     */
    Class<?> type();
}
