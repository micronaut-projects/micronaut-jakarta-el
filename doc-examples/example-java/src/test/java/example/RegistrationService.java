package example;

import io.micronaut.el.example.eligible.Eligible;
import io.micronaut.el.example.eligible.MinAmount;
import jakarta.inject.Singleton;

@Singleton
public class RegistrationService {

    @Eligible(value = "#{ fn:adult(customer.age) && fn:inEurope(customer.country) }", // <1>
              otherwise = "#{ customer.name += ' must be an adult in Europe' }", // <2>
              name = "REGISTER") // <3>
    public String register(Customer customer) {
        return "registered " + customer.name();
    }

    @Eligible("${ customer.country == Locale.GERMANY.country }") // <4>
    public String deposit(Customer customer,
                          @MinAmount(value = 100, inclusive = true,
                                     message = "Must be greater than ${inclusive == true ? 'or equal to ' : ''}{value}") // <5>
                          long amount) {
        return "deposited " + amount + " for " + customer.name();
    }
}
