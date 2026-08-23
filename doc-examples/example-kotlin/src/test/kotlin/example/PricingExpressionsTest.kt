package example

import io.micronaut.context.ApplicationContext
import io.micronaut.el.CompiledELContext
import io.micronaut.el.ELBeanProvider
import jakarta.el.ELException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PricingExpressionsTest {

    private val book = Book("Jakarta EL", "reference", 20.0)

    @Test
    fun theFunctionIsInvokedOnTheBean() {
        ApplicationContext.run().use { applicationContext ->
            val context = CompiledELContext().setBean("book", book)
            context.putContext(ELBeanProvider::class.java, object : ELBeanProvider { // <1>
                override fun <T : Any> get(type: Class<T>): T = applicationContext.getBean(type)
            })

            assertEquals(54.0, `PricingExpressions$ELExpressions`.QUOTE.getValue(context)) // <2>
            assertEquals("54.0 EUR", `PricingExpressions$ELExpressions`.PRICED.getValue(context))
        }
    }

    @Test
    fun anInstanceCanBeRegisteredDirectly() {
        val context = CompiledELContext().setBean("book", book)
        context.putContext(PricingService::class.java, PricingService()) // <3>

        assertEquals(54.0, `PricingExpressions$ELExpressions`.QUOTE.getValue(context))
    }

    @Test
    fun withoutAnInstanceTheEvaluationFails() {
        val context = CompiledELContext().setBean("book", book)

        val failure = assertThrows(ELException::class.java) { `PricingExpressions$ELExpressions`.QUOTE.getValue<Any>(context) } // <4>
        assertTrue(failure.message!!.contains("No instance of example.PricingService"), failure.message)
    }
}
