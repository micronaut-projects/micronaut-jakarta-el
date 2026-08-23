package io.micronaut.el.test.eligible;

import io.micronaut.el.example.eligible.Eligible;
import jakarta.inject.Singleton;

@Singleton
public class RegistrationService {

    @Eligible(value = "#{ fn:adult(customer.age) && fn:inEurope(customer.country) }",
              otherwise = "#{ customer.name += ' must be an adult in Europe' }",
              name = "REGISTER")
    public String register(Customer customer) {
        return "registered " + customer.name();
    }

    @Eligible(value = "${ amount > 0 && customer.country == Locale.GERMANY.country }")
    public String deposit(Customer customer, long amount) {
        return "deposited " + amount + " for " + customer.name();
    }
}
