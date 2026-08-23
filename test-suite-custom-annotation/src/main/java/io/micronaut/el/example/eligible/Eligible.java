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
 * @Eligible("#{ customer.age >= 18 && customer.country == 'CZ' }")
 * public String register(Customer customer) { ... }
 * }</pre>
 *
 * <p>The condition is compiled at compilation time by {@link EligibleRemapper}, and evaluated by an interceptor
 * before the method runs. Both {@code #{...}} and {@code ${...}} delimiters are accepted.</p>
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
}
