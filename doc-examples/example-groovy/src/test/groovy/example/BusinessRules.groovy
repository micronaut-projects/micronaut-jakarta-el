package example

import io.micronaut.el.example.eligible.Eligible
import io.micronaut.el.example.eligible.MinAmount
import jakarta.inject.Singleton

@Singleton
class BusinessRules {

    // tag::register[]
    @Eligible(value = '#{ fn:adult(customer.age) && fn:inEurope(customer.country) }',
              otherwise = "#{ customer.name += ' must be an adult in Europe' }")
    String register(Customer customer) {
        "registered " + customer.name
    }
    // end::register[]

    // tag::deposit[]
    @Eligible('#{ amount > 0 }')
    String deposit(@MinAmount(value = 100L) long amount) {
        // its default message: "Must be greater than ${inclusive == true ? 'or equal to ' : ''}{value}"
        "deposited " + amount
    }
    // end::deposit[]
}
