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

import io.micronaut.core.annotation.Experimental;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps the public static methods of a class to Jakarta Expression Language functions.
 *
 * <p>Functions are resolved and bound at compilation time, which replaces the
 * {@code jakarta.el.FunctionMapper} lookup performed by an interpreting implementation.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 * @see ELFunction
 * @see ELEnvironment
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
@Experimental
public @interface ELFunctions {

    /**
     * @return The class declaring the public static methods to expose as functions
     */
    Class<?> value();

    /**
     * @return The namespace prefix of the functions, empty for functions without a namespace
     */
    String prefix() default "";
}
