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

import io.micronaut.core.annotation.Internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class generated in the package {@code io.micronaut.el.functions} for every class declaring functions
 * with {@link ELFunction}, so that the processor of another module finds them. The generated class is named
 * {@code Index_} followed by the name of the declaring class with every dot replaced by an underscore: the
 * classpath scanner of Groovy skips the classes whose name contains a dollar sign.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ELFunctionIndex {

    /**
     * The package of the generated classes.
     */
    String PACKAGE = "io.micronaut.el.functions";

    /**
     * @return The name of the class declaring the functions
     */
    String value();
}
