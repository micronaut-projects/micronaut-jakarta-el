package example;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.BeanDefinitionRegistry;
import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutableMethodsTest {

    @Test
    void anExecutableMethodIsInvokedWithoutReflection() {
        try (ApplicationContext beanContext = ApplicationContext.run()) {
            ELContext context = new CompiledELContext().setBean("greeter", beanContext.getBean(Greeter.class));
            context.putContext(BeanDefinitionRegistry.class, beanContext); // <1>

            assertEquals("Hello world", ELManager.getExpressionFactory()
                .createValueExpression(context, "${greeter.greet('world')}", String.class).getValue(context)); // <2>
        }
    }
}
