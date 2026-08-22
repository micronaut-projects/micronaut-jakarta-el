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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the compilation time environment shared by the expressions of the annotated type.
 *
 * <p>The environment is the compilation time counterpart of the {@code jakarta.el.ELContext}: it
 * provides the variable types, the imported classes and the functions that are known when the
 * expressions are compiled.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface ELEnvironment {

    /**
     * @return The statically typed variables available to the expressions
     */
    ELVariable[] variables() default {};

    /**
     * @return The classes imported into the expression environment
     */
    Class<?>[] imports() default {};

    /**
     * @return The packages imported into the expression environment
     */
    String[] importPackages() default {};

    /**
     * @return The classes whose public static fields and methods are imported statically
     */
    Class<?>[] staticImports() default {};

    /**
     * @return The function namespaces available to the expressions
     */
    ELFunctions[] functions() default {};
}
