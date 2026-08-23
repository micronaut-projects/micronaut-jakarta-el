package example

import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.el.CompiledELContext
import io.micronaut.core.annotation.AnnotationValue
import io.micronaut.core.type.Argument
import io.micronaut.el.example.eligible.ConstraintMessages
import io.micronaut.el.example.eligible.Eligible
import io.micronaut.el.example.eligible.MinAmount
import jakarta.el.ELManager
import jakarta.el.ValueExpression
import jakarta.inject.Singleton

import java.util.concurrent.ConcurrentHashMap

@Singleton
@InterceptorBean(Eligible) // <1>
class EligibleInterceptor implements MethodInterceptor<Object, Object> {

    private final Map<String, ValueExpression> expressions = new ConcurrentHashMap<>()

    @Override
    Object intercept(MethodInvocationContext<Object, Object> context) {
        CompiledELContext elContext = new CompiledELContext()
        context.parameterValueMap.each { name, value -> elContext.setBean(name, value) } // <2>

        for (Argument<?> argument : context.arguments) { // <5>
            AnnotationValue<MinAmount> constraint = argument.annotationMetadata.getAnnotation(MinAmount)
            Object value = context.parameterValueMap[argument.name]
            if (constraint != null && value instanceof Number && !satisfies(constraint, ((Number) value).longValue())) {
                throw new NotEligibleException(ConstraintMessages.interpolate(constraint, value)) // <6>
            }
        }

        String condition = context.stringValue(Eligible).orElseThrow() // <3>
        if (expression(elContext, condition, Boolean).getValue(elContext) == Boolean.TRUE) {
            return context.proceed()
        }
        String otherwise = context.stringValue(Eligible, "otherwise").orElse("")
        String message = otherwise.isEmpty()
            ? "${context.methodName} requires ${condition}".toString()
            : expression(elContext, otherwise, String).getValue(elContext)
        throw new NotEligibleException(message)
    }

    private static boolean satisfies(AnnotationValue<MinAmount> constraint, long amount) {
        long minimum = constraint.longValue().orElseThrow()
        constraint.booleanValue("inclusive").orElse(false) ? amount >= minimum : amount > minimum
    }

    private ValueExpression expression(CompiledELContext elContext, String text, Class<?> expectedType) {
        expressions.computeIfAbsent(text) { key ->
            ELManager.expressionFactory.createValueExpression(elContext, key, expectedType) // <4>
        }
    }
}
