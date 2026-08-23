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
import io.micronaut.context.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a method as a Jakarta Expression Language function, with its own name and namespace prefix.
 *
 * <p>An annotated function is found by the expressions of the module it is declared in without any declaration;
 * the functions of another module are listed with {@link ELFunctions}, which also exposes all the public static
 * methods, and the public instance methods, of a class with no annotated method. Once a method of a class
 * carries this annotation, only the annotated methods of the class are functions: a bean exposing one operation
 * does not expose every public method it has.</p>
 *
 * <p>A static function is invoked directly. An instance function is invoked on the instance the
 * {@code jakarta.el.ELContext} provides at evaluation time, see {@code io.micronaut.el.ELBeanProvider}.</p>
 *
 * <pre>{@code
 * &#64;Singleton
 * public class PricingService {
 *
 *     &#64;ELFunction(prefix = "pricing")
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
     * @return The local name of the function, an alias of {@link #value()}
     */
    @AliasFor(member = "value")
    String name() default "";

    /**
     * @return The namespace prefix of the function, defaults to the prefix {@link ELFunctions} gives the class
     */
    String prefix() default "";
}
