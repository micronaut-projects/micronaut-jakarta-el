package example;

import io.micronaut.context.ApplicationContext;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.ELBeanProvider;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingExpressionsTest {

    private final Book book = new Book("Jakarta EL", "reference", 20d);

    @Test
    void theFunctionIsInvokedOnTheBean() {
        try (ApplicationContext applicationContext = ApplicationContext.run()) {
            ELContext context = new CompiledELContext().setBean("book", book);
            context.putContext(ELBeanProvider.class, (ELBeanProvider) applicationContext::getBean); // <1>

            assertEquals(54d, PricingExpressions$ELExpressions.QUOTE.getValue(context)); // <2>
            assertEquals("54.0 EUR", PricingExpressions$ELExpressions.PRICED.getValue(context));
        }
    }

    @Test
    void anInstanceCanBeRegisteredDirectly() {
        ELContext context = new CompiledELContext().setBean("book", book);
        context.putContext(PricingService.class, new PricingService()); // <3>

        assertEquals(54d, PricingExpressions$ELExpressions.QUOTE.getValue(context));
    }

    @Test
    void withoutAnInstanceTheEvaluationFails() {
        ELContext context = new CompiledELContext().setBean("book", book);

        ELException failure = assertThrows(ELException.class, () -> PricingExpressions$ELExpressions.QUOTE.getValue(context)); // <4>
        assertTrue(failure.getMessage().contains("No instance of example.PricingService"), failure.getMessage());
    }
}
