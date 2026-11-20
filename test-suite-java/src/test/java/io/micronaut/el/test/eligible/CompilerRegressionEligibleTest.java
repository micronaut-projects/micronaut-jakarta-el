package io.micronaut.el.test.eligible;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.el.example.eligible.ConstraintMessages;
import io.micronaut.el.example.eligible.Eligible;
import io.micronaut.el.example.eligible.MinAmount;
import io.micronaut.inject.BeanDefinition;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The remappers of the custom annotation store the expressions the way the processor compiles them.
 */
class CompilerRegressionEligibleTest {

    @Test
    void theAliasOfTheConditionAndEveryDelimiterOfACompositeAreRemapped() {
        // the condition is written through the alias, and the message holds two expressions
        ValueExpression condition = CompilerRegressionEligibleService$ELExpressions.ALIASED;
        assertEquals("#{ amount > 0 }", condition.getExpressionString());
        ValueExpression otherwise = CompilerRegressionEligibleService$ELExpressions.ALIASED_OTHERWISE;
        assertEquals("Sorry #{customer.name}, #{amount} is not enough", otherwise.getExpressionString());
        try (ApplicationContext context = ApplicationContext.run()) {
            CompilerRegressionEligibleService service = context.getBean(CompilerRegressionEligibleService.class);
            assertEquals("paid 3", service.pay(new Customer("Ann", 34, "DE"), 3));
            NotEligibleException rejected = assertThrows(NotEligibleException.class,
                () -> service.pay(new Customer("Ann", 34, "DE"), 0));
            assertEquals("Sorry Ann, 0 is not enough", rejected.getMessage());
            BeanDefinition<CompilerRegressionEligibleService> definition =
                context.getBeanDefinition(CompilerRegressionEligibleService.class);
            assertEquals("#{ amount > 0 }", definition.getRequiredMethod("pay", Customer.class, long.class)
                .stringValue(Eligible.class).orElseThrow());
        }
    }

    @Test
    void anEscapedDelimiterOfAMessageStaysLiteralText() {
        try (ApplicationContext context = ApplicationContext.run()) {
            BeanDefinition<CompilerRegressionEligibleService> definition =
                context.getBeanDefinition(CompilerRegressionEligibleService.class);
            AnnotationValue<MinAmount> constraint = definition.getRequiredMethod("transfer", long.class)
                .getArguments()[0].getAnnotationMetadata().getAnnotation(MinAmount.class);
            assertEquals("Costs \\${fee} at least {value}, got #{validatedValue}",
                constraint.stringValue("message").orElseThrow());
            assertEquals("Costs ${fee} at least 5, got 3", ConstraintMessages.interpolate(constraint, 3L));
        }
    }
}
