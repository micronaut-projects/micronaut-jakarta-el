package example

import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanDefinitionRegistry
import io.micronaut.el.CompiledELContext
import jakarta.el.ELManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExecutableMethodsTest {

    @Test
    fun anExecutableMethodIsInvokedWithoutReflection() {
        ApplicationContext.run().use { beanContext ->
            val context = CompiledELContext().setBean("greeter", beanContext.getBean(Greeter::class.java))
            context.putContext(BeanDefinitionRegistry::class.java, beanContext) // <1>

            assertEquals("Hello world", ELManager.getExpressionFactory()
                .createValueExpression(context, "\${greeter.greet('world')}", String::class.java).getValue(context)) // <2>
        }
    }
}
