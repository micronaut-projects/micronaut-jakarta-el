package example;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.el.CompiledELContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.type.Argument;
import io.micronaut.el.example.eligible.ConstraintMessages;
import io.micronaut.el.example.eligible.Eligible;
import io.micronaut.el.example.eligible.MinAmount;
import jakarta.el.ELManager;
import jakarta.el.ValueExpression;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@InterceptorBean(Eligible.class) // <1>
public class EligibleInterceptor implements MethodInterceptor<Object, Object> {

    private final Map<String, ValueExpression> expressions = new ConcurrentHashMap<>();

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        CompiledELContext elContext = new CompiledELContext();
        context.getParameterValueMap().forEach(elContext::setBean); // <2>

        for (Argument<?> argument : context.getArguments()) { // <5>
            AnnotationValue<MinAmount> constraint = argument.getAnnotationMetadata().getAnnotation(MinAmount.class);
            Object value = context.getParameterValueMap().get(argument.getName());
            if (constraint != null && value instanceof Number number && !satisfies(constraint, number.longValue())) {
                throw new NotEligibleException(ConstraintMessages.interpolate(constraint, value)); // <6>
            }
        }

        String condition = context.stringValue(Eligible.class).orElseThrow(); // <3>
        if (Boolean.TRUE.equals(expression(elContext, condition, Boolean.class).getValue(elContext))) {
            return context.proceed();
        }
        String message = context.stringValue(Eligible.class, "otherwise")
            .filter(otherwise -> !otherwise.isEmpty())
            .map(otherwise -> expression(elContext, otherwise, String.class).<String>getValue(elContext))
            .orElse(context.getMethodName() + " requires " + condition);
        throw new NotEligibleException(message);
    }

    private static boolean satisfies(AnnotationValue<MinAmount> constraint, long amount) {
        long minimum = constraint.longValue().orElseThrow();
        return constraint.booleanValue("inclusive").orElse(false) ? amount >= minimum : amount > minimum;
    }

    private ValueExpression expression(CompiledELContext elContext, String text, Class<?> expectedType) {
        return expressions.computeIfAbsent(text, key ->
            ELManager.getExpressionFactory().createValueExpression(elContext, key, expectedType)); // <4>
    }
}
