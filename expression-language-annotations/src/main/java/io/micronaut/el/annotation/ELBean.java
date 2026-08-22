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
 * Marks a type as a Jakarta Expression Language bean.
 *
 * <p>For every annotated type a {@code jakarta.el.ELResolver} is generated at compilation time. The
 * generated resolver resolves the bean properties and methods with direct invocations, replacing the
 * reflective lookups performed by {@code jakarta.el.BeanELResolver}.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface ELBean {

    /**
     * The property names to include. When empty all the readable bean properties are included.
     *
     * @return The included property names
     */
    String[] includes() default {};

    /**
     * The property names to exclude.
     *
     * @return The excluded property names
     */
    String[] excludes() default {};

    /**
     * Whether the public methods of the type should be resolvable with the EL method invocation syntax.
     *
     * @return True if methods should be resolvable
     */
    boolean methods() default true;
}
