package io.micronaut.el.test.processor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The user's marker: the given members of the annotated annotation hold Jakarta Expression Language text, either
 * a single {@code #{...}} expression or a message template mixing text and {@code #{...}} expressions.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface JakartaExpLang {

    /**
     * @return The members holding expressions
     */
    String[] members() default "value";

    /**
     * @return The expected type the expressions evaluate to
     */
    Class<?> expectedType() default Object.class;
}
