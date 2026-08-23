package io.micronaut.el.test.eligible;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.example.eligible.Eligible;
import jakarta.el.ELManager;
import jakarta.el.ValueExpression;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Evaluates the condition of {@link Eligible} before the method runs, with the parameters of the invocation
 * bound by name, and the message of the rejection when it does not hold. The expressions come precompiled from
 * the registry: {@code createValueExpression} is a lookup by text, not a parse, done once per expression.
 */
@Singleton
@InterceptorBean(Eligible.class)
public class EligibleInterceptor implements MethodInterceptor<Object, Object> {

    private final Map<String, ValueExpression> expressions = new ConcurrentHashMap<>();

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        CompiledELContext elContext = new CompiledELContext();
        context.getParameterValueMap().forEach(elContext::setBean);

        String condition = context.stringValue(Eligible.class).orElseThrow();
        if (Boolean.TRUE.equals(expression(elContext, condition, Boolean.class).getValue(elContext))) {
            return context.proceed();
        }
        String message = context.stringValue(Eligible.class, "otherwise")
            .filter(otherwise -> !otherwise.isEmpty())
            .map(otherwise -> expression(elContext, otherwise, String.class).<String>getValue(elContext))
            .orElse(context.getMethodName() + " requires " + condition);
        throw new NotEligibleException(message);
    }

    private ValueExpression expression(CompiledELContext elContext, String text, Class<?> expectedType) {
        return expressions.computeIfAbsent(text, key ->
            ELManager.getExpressionFactory().createValueExpression(elContext, key, expectedType));
    }

    /**
     * @return The compiled expressions evaluated so far, by their text
     */
    public Map<String, ValueExpression> getExpressions() {
        return expressions;
    }
}
