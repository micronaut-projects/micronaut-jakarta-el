package example

import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.el.CompiledELContext
import io.micronaut.core.annotation.AnnotationValue
import io.micronaut.el.example.eligible.ConstraintMessages
import io.micronaut.el.example.eligible.Eligible
import io.micronaut.el.example.eligible.MinAmount
import jakarta.el.ELManager
import jakarta.el.ValueExpression
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap

@Singleton
@InterceptorBean(Eligible::class) // <1>
class EligibleInterceptor : MethodInterceptor<Any, Any> {

    private val expressions = ConcurrentHashMap<String, ValueExpression>()

    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {
        val elContext = CompiledELContext()
        context.parameterValueMap.forEach { (name, value) -> elContext.setBean(name, value) } // <2>

        for (argument in context.arguments) { // <5>
            val constraint = argument.annotationMetadata.getAnnotation(MinAmount::class.java)
            val value = context.parameterValueMap[argument.name]
            if (constraint != null && value is Number && !satisfies(constraint, value.toLong())) {
                throw NotEligibleException(ConstraintMessages.interpolate(constraint, value)) // <6>
            }
        }

        val condition = context.stringValue(Eligible::class.java).orElseThrow() // <3>
        if (expression(elContext, condition, Boolean::class.javaObjectType).getValue<Boolean?>(elContext) == true) {
            return context.proceed()
        }
        val otherwise = context.stringValue(Eligible::class.java, "otherwise").orElse("")
        val message = if (otherwise.isEmpty()) "${context.methodName} requires $condition"
            else expression(elContext, otherwise, String::class.java).getValue<String>(elContext)
        throw NotEligibleException(message)
    }

    private fun satisfies(constraint: AnnotationValue<MinAmount>, amount: Long): Boolean {
        val minimum = constraint.longValue().orElseThrow()
        return if (constraint.booleanValue("inclusive").orElse(false)) amount >= minimum else amount > minimum
    }

    private fun expression(elContext: CompiledELContext, text: String, expectedType: Class<*>): ValueExpression =
        expressions.computeIfAbsent(text) { key ->
            ELManager.getExpressionFactory().createValueExpression(elContext, key, expectedType) // <4>
        }
}
