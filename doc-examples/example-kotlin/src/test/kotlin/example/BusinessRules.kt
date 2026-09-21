package example

import io.micronaut.el.example.eligible.Eligible
import io.micronaut.el.example.eligible.MinAmount
import jakarta.inject.Singleton

@Singleton
open class BusinessRules {

    // tag::register[]
    @Eligible(value = "#{ fn:adult(customer.age) && fn:inEurope(customer.country) }",
              otherwise = "#{ customer.name += ' must be an adult in Europe' }")
    open fun register(customer: Customer): String = "registered " + customer.name
    // end::register[]

    // tag::deposit[]
    @Eligible("#{ amount > 0 }")
    open fun deposit(@MinAmount(value = 100) amount: Long): String =
        // its default message: "Must be greater than ${inclusive == true ? 'or equal to ' : ''}{value}"
        "deposited $amount"
    // end::deposit[]
}
