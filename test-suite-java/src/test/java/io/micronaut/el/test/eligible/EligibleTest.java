package io.micronaut.el.test.eligible;

import io.micronaut.context.ApplicationContext;
import io.micronaut.el.example.eligible.Eligible;
import io.micronaut.el.runtime.CompiledExpression;
import io.micronaut.inject.BeanDefinition;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A custom annotation, its annotation processor, and a runtime evaluating the precompiled expressions.
 */
class EligibleTest {

    @Test
    void theConditionGuardsTheMethodAndTheMessageExplainsTheRejection() {
        try (ApplicationContext context = ApplicationContext.run()) {
            RegistrationService service = context.getBean(RegistrationService.class);

            assertEquals("registered Ann", service.register(new Customer("Ann", 34, "CZ")));
            NotEligibleException minor = assertThrows(NotEligibleException.class,
                () -> service.register(new Customer("Bob", 15, "CZ")));
            assertEquals("Bob must be an adult in Europe", minor.getMessage());
            assertThrows(NotEligibleException.class, () -> service.register(new Customer("Cy", 40, "US")));

            assertEquals("deposited 50 for Ann", service.deposit(new Customer("Ann", 34, "DE"), 50));
            NotEligibleException zero = assertThrows(NotEligibleException.class,
                () -> service.deposit(new Customer("Ann", 34, "DE"), 0));
            assertEquals("deposit requires #{ amount > 0 && customer.country == Locale.GERMANY.country }", zero.getMessage());
        }
    }

    @Test
    void everyExpressionIsPrecompiled() {
        try (ApplicationContext context = ApplicationContext.run()) {
            RegistrationService service = context.getBean(RegistrationService.class);
            service.register(new Customer("Ann", 34, "CZ"));
            assertThrows(NotEligibleException.class, () -> service.register(new Customer("Bob", 15, "CZ")));
            service.deposit(new Customer("Ann", 34, "DE"), 1);

            EligibleInterceptor interceptor = context.getBean(EligibleInterceptor.class);
            assertEquals(3, interceptor.getExpressions().size());
            interceptor.getExpressions().forEach((text, expression) ->
                assertTrue(expression instanceof CompiledExpression, "not compiled: " + text));
        }
    }

    @Test
    void theNameAndTheExpectedTypeReachTheGeneratedConstants() {
        // the name given to the annotation names the constant of the registry, typed as the remapper declared
        ValueExpression condition = RegistrationService$ELExpressions.REGISTER;
        assertEquals(Boolean.class, condition.getExpectedType());
        assertEquals("#{ fn:adult(customer.age) && fn:inEurope(customer.country) }", condition.getExpressionString());
        ValueExpression otherwise = RegistrationService$ELExpressions.REGISTER_OTHERWISE;
        assertEquals(String.class, otherwise.getExpectedType());
        assertEquals("#{ customer.name += ' must be an adult in Europe' }", otherwise.getExpressionString());
    }

    @Test
    void theProcessorKeptTheTextAndMicronautCompiledNothing() {
        try (ApplicationContext context = ApplicationContext.run()) {
            BeanDefinition<RegistrationService> definition = context.getBeanDefinition(RegistrationService.class);
            assertEquals("#{ fn:adult(customer.age) && fn:inEurope(customer.country) }",
                definition.getRequiredMethod("register", Customer.class).stringValue(Eligible.class).orElseThrow());
            // written with ${...}, stored as #{...}: the same expression to the specification, and not a property
            // placeholder to Micronaut
            assertEquals("#{ amount > 0 && customer.country == Locale.GERMANY.country }",
                definition.getRequiredMethod("deposit", Customer.class, long.class).stringValue(Eligible.class).orElseThrow());
        }
        // Micronaut never saw an expression of its own
        assertThrows(ClassNotFoundException.class, () -> Class.forName("io.micronaut.el.test.eligible.$RegistrationService$Expr0"));
    }
}
