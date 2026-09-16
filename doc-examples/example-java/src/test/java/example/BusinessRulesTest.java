package example;

import io.micronaut.context.ApplicationContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessRulesTest {

    @Test
    void theRulesGuardTheMethods() {
        try (ApplicationContext context = ApplicationContext.run()) {
            BusinessRules rules = context.getBean(BusinessRules.class);

            assertEquals("registered Ann", rules.register(new Customer("Ann", 34, "CZ")));
            NotEligibleException rejected = assertThrows(NotEligibleException.class,
                () -> rules.register(new Customer("Bob", 15, "CZ")));
            assertEquals("Bob must be an adult in Europe", rejected.getMessage());

            assertEquals("deposited 101", rules.deposit(101));
            NotEligibleException tooSmall = assertThrows(NotEligibleException.class, () -> rules.deposit(100));
            assertEquals("Must be greater than 100", tooSmall.getMessage());
        }
    }
}
