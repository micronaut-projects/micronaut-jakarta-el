package io.micronaut.el.example.eligible;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A constraint in the style of Jakarta Validation: its message is a template mixing literal text, the
 * {@code {value}} attribute of the constraint and the {@code ${...}} expressions of the specification, which
 * see the attributes of the constraint and the validated value as variables:
 *
 * <pre>{@code
 * @MinAmount(value = 100, inclusive = true,
 *            message = "Must be greater than ${inclusive == true ? 'or equal to ' : ''}{value}")
 * }</pre>
 *
 * <p>The expressions are compiled at compilation time by {@link MinAmountRemapper}, typed from the members of the
 * annotation, and the message is interpolated by the interceptor of {@link Eligible} when the constraint is
 * violated: the attributes first, the expressions next, as the Bean Validation specification orders it.</p>
 */
// tag::annotation[]
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface MinAmount {

    /**
     * @return The minimum
     */
    long value();

    /**
     * @return Whether the minimum itself is accepted
     */
    boolean inclusive() default false;

    /**
     * @return The message of the violation, a template over {@code {value}}, {@code {inclusive}} and the
     * expressions of the specification
     */
    String message() default "Must be greater than ${inclusive == true ? 'or equal to ' : ''}{value}"; // <1>
}
// end::annotation[]
