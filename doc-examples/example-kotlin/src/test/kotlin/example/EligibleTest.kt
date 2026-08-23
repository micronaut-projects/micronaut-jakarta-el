package example

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class EligibleTest {

    @Test
    fun theConditionGuardsTheMethod() {
        ApplicationContext.run().use { context ->
            val service = context.getBean(RegistrationService::class.java)

            assertEquals("registered Ann", service.register(Customer("Ann", 34, "CZ"))) // <1>
            val rejected = assertThrows(NotEligibleException::class.java) {
                service.register(Customer("Bob", 15, "CZ"))
            }
            assertEquals("Bob must be an adult in Europe", rejected.message) // <2>

            assertEquals("deposited 100 for Ann", service.deposit(Customer("Ann", 34, "DE"), 100))
            val tooSmall = assertThrows(NotEligibleException::class.java) {
                service.deposit(Customer("Ann", 34, "DE"), 99)
            }
            assertEquals("Must be greater than or equal to 100", tooSmall.message) // <4>
        }
    }

    @Test
    fun theGeneratedConstantsCarryTheNameAndTheExpectedType() {
        val condition = `RegistrationService$ELExpressions`.REGISTER // <3>
        assertEquals(Boolean::class.javaObjectType, condition.expectedType)
        val otherwise = `RegistrationService$ELExpressions`.REGISTER_OTHERWISE
        assertEquals(String::class.java, otherwise.expectedType)
    }
}
