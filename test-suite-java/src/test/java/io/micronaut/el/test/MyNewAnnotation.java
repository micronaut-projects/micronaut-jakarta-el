package io.micronaut.el.test;

import io.micronaut.el.test.processor.JakartaExpLang;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A user annotation whose value is a Jakarta Expression Language expression rather than a Micronaut one.
 */
@JakartaExpLang(expectedType = String.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MyNewAnnotation {

    String value();
}
