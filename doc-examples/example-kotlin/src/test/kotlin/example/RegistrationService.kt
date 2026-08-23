package example

import io.micronaut.el.example.eligible.Eligible
import io.micronaut.el.example.eligible.MinAmount
import jakarta.inject.Singleton

@Singleton
open class RegistrationService {

    @Eligible(value = "#{ fn:adult(customer.age) && fn:inEurope(customer.country) }", // <1>
              otherwise = "#{ customer.name += ' must be an adult in Europe' }", // <2>
              name = "REGISTER") // <3>
    open fun register(customer: Customer): String = "registered " + customer.name

    @Eligible("\${ customer.country == Locale.GERMANY.country }") // <4>
    open fun deposit(customer: Customer,
                     @MinAmount(value = 100, inclusive = true,
                                message = "Must be greater than \${inclusive == true ? 'or equal to ' : ''}{value}") // <5>
                     amount: Long): String = "deposited $amount for " + customer.name
}
