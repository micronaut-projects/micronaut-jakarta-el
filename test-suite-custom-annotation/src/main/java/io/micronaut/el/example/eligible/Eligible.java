package io.micronaut.el.example.eligible;

import io.micronaut.aop.Around;
import io.micronaut.aop.InterceptorBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Guards a method with a Jakarta Expression Language condition over its parameters, referenced by name:
 *
 * <pre>{@code
 * @Eligible(value = "#{ fn:adult(customer.age) && customer.country == Locale.GERMANY.country }",
 *           otherwise = "#{ customer.name += ' is not eligible' }")
 * public String register(Customer customer) { ... }
 * }</pre>
 *
 * <p>The condition and the message are compiled at compilation time by {@link EligibleRemapper}. The parameters
 * of the method are typed variables; the functions of {@link EligibilityFunctions} are available under the
 * {@code fn} prefix and {@code java.util.Locale} is imported. Both {@code #{...}} and {@code ${...}} delimiters
 * are accepted.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Around
@InterceptorBinding
public @interface Eligible {

    /**
     * @return The condition the parameters of the method must satisfy
     */
    String value();

    /**
     * @return The message of the rejection, an expression evaluating to a string, or empty for a default one
     */
    String otherwise() default "";

    /**
     * @return The name of the constant holding the compiled condition in the registry of the class, for the
     * code that wants to reference it directly; derived from the condition when empty
     */
    String name() default "";
}
