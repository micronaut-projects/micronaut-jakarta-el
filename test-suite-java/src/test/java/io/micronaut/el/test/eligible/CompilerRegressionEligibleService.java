package io.micronaut.el.test.eligible;

import io.micronaut.el.example.eligible.Eligible;
import io.micronaut.el.example.eligible.MinAmount;
import io.micronaut.context.annotation.Executable;
import jakarta.inject.Singleton;

/**
 * The remappers of the custom annotation regressions: the alias of the condition is read, every
 * {@code ${...}} of a composite expression is stored as {@code #{...}}, and an escaped {@code \${} of a
 * message template stays literal text.
 */
@Singleton
public class CompilerRegressionEligibleService {

    @Eligible(condition = "${ amount > 0 }",
              otherwise = "Sorry ${customer.name}, ${amount} is not enough",
              name = "ALIASED")
    public String pay(Customer customer, long amount) {
        return "paid " + amount;
    }

    @Executable
    public String transfer(@MinAmount(value = 5, message = "Costs \\${fee} at least {value}, got ${validatedValue}") long amount) {
        return "transferred " + amount;
    }
}
