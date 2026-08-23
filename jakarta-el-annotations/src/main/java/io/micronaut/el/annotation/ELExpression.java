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
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a Jakarta Expression Language value expression to be compiled at compilation time.
 *
 * <p>A {@code jakarta.el.ValueExpression} implementation is generated for the expression, so that no
 * parsing, no abstract syntax tree traversal and, whenever the types are known, no reflective
 * resolution happens at runtime.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 * @see ELEnvironment
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Repeatable(ELExpressions.class)
@Experimental
public @interface ELExpression {

    /**
     * The expression, which can be an eval-expression such as {@code ${user.name}}, a literal-expression
     * or a composite expression as defined by the Jakarta Expression Language specification.
     *
     * @return The expression
     */
    String value();

    /**
     * @return The expected type the evaluation result is coerced to
     */
    Class<?> expectedType() default Object.class;

    /**
     * @return The name of the generated constant, derived from the expression when not specified
     */
    String name() default "";
}
