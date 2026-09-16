from typing import Annotated

from jakarta.inject import Singleton
from micronaut.el.example.eligible import Eligible, MinAmount

from example.Customer import Customer


@Singleton
class RegistrationService:

    @Eligible(value="#{ fn:adult(customer.age) && fn:inEurope(customer.country) }",  # <1>
              otherwise="#{ customer.name += ' must be an adult in Europe' }",  # <2>
              name="REGISTER")  # <3>
    def register(self, customer: Customer) -> str:
        return "registered " + customer.name

    @Eligible("${ customer.country == Locale.GERMANY.country }")  # <4>
    def deposit(self, customer: Customer,
                amount: Annotated[int, MinAmount(value=100, inclusive=True,
                                                 message="Must be greater than ${inclusive == true ? 'or equal to ' : ''}{value}")]  # <5>
                ) -> str:
        return "deposited " + str(amount) + " for " + customer.name
