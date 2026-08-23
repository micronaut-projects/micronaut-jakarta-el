package example

import io.micronaut.aop.InterceptorBean
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.el.CompiledELContext
import io.micronaut.el.example.eligible.Eligible
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

        val condition = context.stringValue(Eligible::class.java).orElseThrow() // <3>
        if (expression(elContext, condition, Boolean::class.javaObjectType).getValue<Boolean?>(elContext) == true) {
            return context.proceed()
        }
        val otherwise = context.stringValue(Eligible::class.java, "otherwise").orElse("")
        val message = if (otherwise.isEmpty()) "${context.methodName} requires $condition"
            else expression(elContext, otherwise, String::class.java).getValue<String>(elContext)
        throw NotEligibleException(message)
    }

    private fun expression(elContext: CompiledELContext, text: String, expectedType: Class<*>): ValueExpression =
        expressions.computeIfAbsent(text) { key ->
            ELManager.getExpressionFactory().createValueExpression(elContext, key, expectedType) // <4>
        }
}
