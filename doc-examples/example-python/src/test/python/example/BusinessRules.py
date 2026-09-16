from typing import Annotated

from jakarta.inject import Singleton
from micronaut.el.example.eligible import Eligible, MinAmount

from example.Customer import Customer


@Singleton
class BusinessRules:

    # tag::register[]
    @Eligible(value="#{ fn:adult(customer.age) && fn:inEurope(customer.country) }",
              otherwise="#{ customer.name += ' must be an adult in Europe' }")
    def register(self, customer: Customer) -> str:
        return "registered " + customer.name
    # end::register[]

    # tag::deposit[]
    @Eligible("#{ amount > 0 }")
    def deposit(self, amount: Annotated[int, MinAmount(value=100)]) -> str:
        # its default message: "Must be greater than ${inclusive == true ? 'or equal to ' : ''}{value}"
        return "deposited " + str(amount)
    # end::deposit[]
