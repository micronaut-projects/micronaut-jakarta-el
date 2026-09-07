package io.micronaut.el.test.executable;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import jakarta.inject.Singleton;

/**
 * Marks the result of an advised method, so that a test can tell an intercepted invocation from a direct one,
 * and one interception from two.
 */
@Singleton
@InterceptorBean(Shouted.class)
public class ShoutedInterceptor implements MethodInterceptor<Object, Object> {

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Object result = context.proceed();
        return result instanceof String text ? text + "!" : result;
    }
}
