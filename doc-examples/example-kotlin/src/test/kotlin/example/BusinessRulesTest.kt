package example

import io.micronaut.context.ApplicationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BusinessRulesTest {

    @Test
    fun theRulesGuardTheMethods() {
        ApplicationContext.run().use { context ->
            val rules = context.getBean(BusinessRules::class.java)

            assertEquals("registered Ann", rules.register(Customer("Ann", 34, "CZ")))
            val rejected = assertThrows(NotEligibleException::class.java) {
                rules.register(Customer("Bob", 15, "CZ"))
            }
            assertEquals("Bob must be an adult in Europe", rejected.message)

            assertEquals("deposited 101", rules.deposit(101))
            val tooSmall = assertThrows(NotEligibleException::class.java) { rules.deposit(100) }
            assertEquals("Must be greater than 100", tooSmall.message)
        }
    }
}
