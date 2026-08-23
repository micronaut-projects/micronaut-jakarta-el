package example

import io.micronaut.context.ApplicationContext
import io.micronaut.el.CompiledELContext
import jakarta.el.ELContext
import jakarta.el.ELManager
import jakarta.el.ValueExpression
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class EligibleTest {

    @Test
    void theConditionGuardsTheMethod() {
        ApplicationContext.run().withCloseable { context ->
            RegistrationService service = context.getBean(RegistrationService)

            assertEquals("registered Ann", service.register(new Customer("Ann", 34, "CZ"))) // <1>
            NotEligibleException rejected = assertThrows(NotEligibleException) {
                service.register(new Customer("Bob", 15, "CZ"))
            }
            assertEquals("Bob must be an adult in Europe", rejected.message) // <2>

            assertEquals("deposited 100 for Ann", service.deposit(new Customer("Ann", 34, "DE"), 100))
            NotEligibleException tooSmall = assertThrows(NotEligibleException) {
                service.deposit(new Customer("Ann", 34, "DE"), 99)
            }
            assertEquals("Must be greater than or equal to 100", tooSmall.message) // <4>
        }
    }

    @Test
    void theExpressionsCarryTheExpectedType() {
        ELContext context = new CompiledELContext()
        ValueExpression condition = ELManager.expressionFactory.createValueExpression(context,
            '#{ fn:adult(customer.age) && fn:inEurope(customer.country) }', Boolean) // <3>
        assertEquals(Boolean, condition.expectedType)
    }
}
