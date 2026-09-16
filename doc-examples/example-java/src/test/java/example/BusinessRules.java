package example;

import io.micronaut.el.example.eligible.Eligible;
import io.micronaut.el.example.eligible.MinAmount;
import jakarta.inject.Singleton;

@Singleton
public class BusinessRules {

    // tag::register[]
    @Eligible(value = "#{ fn:adult(customer.age) && fn:inEurope(customer.country) }",
              otherwise = "#{ customer.name += ' must be an adult in Europe' }")
    public String register(Customer customer) {
        return "registered " + customer.name();
    }
    // end::register[]

    // tag::deposit[]
    @Eligible("#{ amount > 0 }")
    public String deposit(@MinAmount(value = 100) long amount) {
        // its default message: "Must be greater than ${inclusive == true ? 'or equal to ' : ''}{value}"
        return "deposited " + amount;
    }
    // end::deposit[]
}
