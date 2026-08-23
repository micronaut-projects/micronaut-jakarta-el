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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method of a class listed with {@link ELFunctions} as a Jakarta Expression Language function, with
 * its own name and namespace prefix.
 *
 * <p>Once a method of the class carries this annotation, only the annotated methods of the class are functions:
 * a bean exposing one operation does not expose every public method it has. A class with no annotated method
 * exposes all its public static methods, and the public instance methods it declares.</p>
 *
 * <pre>{@code
 * @Singleton
 * public class PricingService {
 *
 *     @ELFunction(prefix = "pricing")
 *     public double quote(Book book, int quantity) { ... }
 * }
 * }</pre>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Experimental
public @interface ELFunction {

    /**
     * @return The local name of the function, defaults to the method name
     */
    String value() default "";

    /**
     * @return The namespace prefix of the function, defaults to the prefix {@link ELFunctions} gives the class
     */
    String prefix() default "";
}
