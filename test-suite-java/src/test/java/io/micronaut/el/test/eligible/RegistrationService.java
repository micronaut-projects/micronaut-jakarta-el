package io.micronaut.el.test.eligible;

import io.micronaut.el.example.eligible.Eligible;
import jakarta.inject.Singleton;

@Singleton
public class RegistrationService {

    @Eligible("#{ customer.age >= 18 && customer.country == 'CZ' }")
    public String register(Customer customer) {
        return "registered " + customer.name();
    }

    @Eligible("${ amount > 0 && customer.age >= 18 }")
    public String deposit(Customer customer, long amount) {
        return "deposited " + amount + " for " + customer.name();
    }
}
