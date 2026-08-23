package example;

import io.micronaut.context.ApplicationContext;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EligibleTest {

    @Test
    void theConditionGuardsTheMethod() {
        try (ApplicationContext context = ApplicationContext.run()) {
            RegistrationService service = context.getBean(RegistrationService.class);

            assertEquals("registered Ann", service.register(new Customer("Ann", 34, "CZ"))); // <1>
            NotEligibleException rejected = assertThrows(NotEligibleException.class,
                () -> service.register(new Customer("Bob", 15, "CZ")));
            assertEquals("Bob must be an adult in Europe", rejected.getMessage()); // <2>

            assertEquals("deposited 50 for Ann", service.deposit(new Customer("Ann", 34, "DE"), 50));
        }
    }

    @Test
    void theGeneratedConstantsCarryTheNameAndTheExpectedType() {
        ValueExpression condition = RegistrationService$ELExpressions.REGISTER; // <3>
        assertEquals(Boolean.class, condition.getExpectedType());
        ValueExpression otherwise = RegistrationService$ELExpressions.REGISTER_OTHERWISE;
        assertEquals(String.class, otherwise.getExpectedType());
    }
}
