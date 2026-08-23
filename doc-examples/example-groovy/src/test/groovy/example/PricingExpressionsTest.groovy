package example

import io.micronaut.context.ApplicationContext
import io.micronaut.el.CompiledELContext
import io.micronaut.el.ELBeanProvider
import jakarta.el.ELContext
import jakarta.el.ELException
import jakarta.el.ELManager
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

class PricingExpressionsTest {

    private final Book book = new Book("Jakarta EL", "reference", 20d)

    @Test
    void theFunctionIsInvokedOnTheBean() {
        ApplicationContext.run().withCloseable { applicationContext ->
            ELContext context = new CompiledELContext().setBean("book", book)
            context.putContext(ELBeanProvider, applicationContext.&getBean as ELBeanProvider) // <1>

            assertEquals(54d, ELManager.expressionFactory.createValueExpression(context, '${pricing:quote(book, 3)}', double).getValue(context)) // <2>
            assertEquals("54.0 EUR", ELManager.expressionFactory
                .createValueExpression(context, "\${pricing:quote(book, 3) += ' ' += pricing:currency()}", String).getValue(context))
        }
    }

    @Test
    void anInstanceCanBeRegisteredDirectly() {
        ELContext context = new CompiledELContext().setBean("book", book)
        context.putContext(PricingService, new PricingService()) // <3>

        assertEquals(54d, ELManager.expressionFactory.createValueExpression(context, '${pricing:quote(book, 3)}', double).getValue(context))
    }

    @Test
    void withoutAnInstanceTheEvaluationFails() {
        ELContext context = new CompiledELContext().setBean("book", book)

        ELException failure = assertThrows(ELException) {
            ELManager.expressionFactory.createValueExpression(context, '${pricing:quote(book, 3)}', double).getValue(context) // <4>
        }
        assertTrue(failure.message.contains("No instance of example.PricingService"), failure.message)
    }
}
