package example

import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanDefinitionRegistry
import io.micronaut.el.CompiledELContext
import jakarta.el.ELContext
import jakarta.el.ELManager
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class ExecutableMethodsTest {

    @Test
    void anExecutableMethodIsInvokedWithoutReflection() {
        ApplicationContext.run().withCloseable { beanContext ->
            ELContext context = new CompiledELContext().setBean("greeter", beanContext.getBean(Greeter))
            context.putContext(BeanDefinitionRegistry, beanContext) // <1>

            assertEquals("Hello world", ELManager.expressionFactory
                .createValueExpression(context, "\${greeter.greet('world')}", String).getValue(context)) // <2>
        }
    }
}
