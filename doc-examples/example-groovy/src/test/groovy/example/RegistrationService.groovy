package example

import io.micronaut.el.example.eligible.Eligible
import io.micronaut.el.example.eligible.MinAmount
import jakarta.inject.Singleton

@Singleton
class RegistrationService {

    @Eligible(value = '#{ fn:adult(customer.age) && fn:inEurope(customer.country) }', // <1>
              otherwise = "#{ customer.name += ' must be an adult in Europe' }", // <2>
              name = "REGISTER") // <3>
    String register(Customer customer) {
        "registered " + customer.name
    }

    @Eligible('${ customer.country == Locale.GERMANY.country }') // <4>
    String deposit(Customer customer,
                   @MinAmount(value = 100L, inclusive = true,
                              message = "Must be greater than \${inclusive == true ? 'or equal to ' : ''}{value}") // <5>
                   long amount) {
        "deposited " + amount + " for " + customer.name
    }
}
