package io.micronaut.el.interpreter.executable;

import io.micronaut.aop.Around;
import io.micronaut.aop.InterceptorBinding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Advises a bean, so that Micronaut generates a proxy for it and its bean definition is a
 * {@code ProxyBeanDefinition}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Around
@InterceptorBinding
public @interface Shouted {
}
