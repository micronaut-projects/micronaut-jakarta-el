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
 * bound by name. The expression comes precompiled from the registry: {@code createValueExpression} is a lookup
 * by the text of the condition, not a parse, and is done once per method.
 */
@Singleton
@InterceptorBean(Eligible.class)
public class EligibleInterceptor implements MethodInterceptor<Object, Object> {

    private final Map<String, ValueExpression> conditions = new ConcurrentHashMap<>();

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        String condition = context.stringValue(Eligible.class).orElseThrow();
        CompiledELContext elContext = new CompiledELContext();
        context.getParameterValueMap().forEach(elContext::setBean);
        ValueExpression expression = conditions.computeIfAbsent(condition, text ->
            ELManager.getExpressionFactory().createValueExpression(elContext, text, Boolean.class));
        if (!Boolean.TRUE.equals(expression.getValue(elContext))) {
            throw new NotEligibleException(context.getMethodName() + " requires " + condition);
        }
        return context.proceed();
    }

    /**
     * @return The compiled conditions evaluated so far, by their text
     */
    public Map<String, ValueExpression> getConditions() {
        return conditions;
    }
}
