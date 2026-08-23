package io.micronaut.el.test.eligible;

import io.micronaut.context.ApplicationContext;
import io.micronaut.el.runtime.CompiledExpression;
import io.micronaut.inject.BeanDefinition;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A custom annotation, its annotation processor, and a runtime evaluating the precompiled expression.
 */
class EligibleTest {

    @Test
    void theConditionGuardsTheMethod() {
        try (ApplicationContext context = ApplicationContext.run()) {
            RegistrationService service = context.getBean(RegistrationService.class);

            assertEquals("registered Ann", service.register(new Customer("Ann", 34, "CZ")));
            NotEligibleException minor = assertThrows(NotEligibleException.class,
                () -> service.register(new Customer("Bob", 15, "CZ")));
            assertEquals("register requires #{ customer.age >= 18 && customer.country == 'CZ' }", minor.getMessage());
            assertThrows(NotEligibleException.class, () -> service.register(new Customer("Cy", 40, "DE")));

            assertEquals("deposited 50 for Ann", service.deposit(new Customer("Ann", 34, "CZ"), 50));
            assertThrows(NotEligibleException.class, () -> service.deposit(new Customer("Ann", 34, "CZ"), 0));
        }
    }

    @Test
    void theConditionIsPrecompiled() {
        try (ApplicationContext context = ApplicationContext.run()) {
            RegistrationService service = context.getBean(RegistrationService.class);
            service.register(new Customer("Ann", 34, "CZ"));
            service.deposit(new Customer("Ann", 34, "CZ"), 1);

            EligibleInterceptor interceptor = context.getBean(EligibleInterceptor.class);
            assertEquals(2, interceptor.getConditions().size());
            interceptor.getConditions().forEach((text, expression) ->
                assertTrue(expression instanceof CompiledExpression, "not compiled: " + text));
        }
    }

    @Test
    void theProcessorKeptTheTextAndMicronautCompiledNothing() throws Exception {
        try (ApplicationContext context = ApplicationContext.run()) {
            BeanDefinition<RegistrationService> definition = context.getBeanDefinition(RegistrationService.class);
            assertEquals("#{ customer.age >= 18 && customer.country == 'CZ' }",
                definition.getRequiredMethod("register", Customer.class).stringValue(io.micronaut.el.example.eligible.Eligible.class).orElseThrow());
            // written with ${...}, stored as #{...}: the same expression to the specification, and not a property
            // placeholder to Micronaut
            assertEquals("#{ amount > 0 && customer.age >= 18 }",
                definition.getRequiredMethod("deposit", Customer.class, long.class).stringValue(io.micronaut.el.example.eligible.Eligible.class).orElseThrow());
        }
        // Micronaut never saw an expression of its own
        assertThrows(ClassNotFoundException.class, () -> Class.forName("io.micronaut.el.test.eligible.$RegistrationService$Expr0"));
    }
}
