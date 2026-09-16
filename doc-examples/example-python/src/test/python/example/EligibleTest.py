from typing import Annotated

import java
from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.Customer import Customer
from example.RegistrationService import RegistrationService

RegistrationService_ELExpressions = java.type("example.RegistrationService$ELExpressions")
# TODO(python): the exception raised by the Python interceptor reaches the caller as the Java class generated for it
NotEligibleException = java.type("example.NotEligibleException")


@MicronautTest
class EligibleTest:

    service: Annotated[RegistrationService, Inject]

    @Test
    def test_the_condition_guards_the_method(self):
        assert self.service.register(Customer("Ann", 34, "CZ")) == "registered Ann"  # <1>
        try:
            self.service.register(Customer("Bob", 15, "CZ"))
        except NotEligibleException as rejected:
            assert rejected.getMessage() == "Bob must be an adult in Europe"  # <2>
        else:
            assert False, "the registration should have been rejected"

        assert self.service.deposit(Customer("Ann", 34, "DE"), 100) == "deposited 100 for Ann"
        try:
            self.service.deposit(Customer("Ann", 34, "DE"), 99)
        except NotEligibleException as too_small:
            assert too_small.getMessage() == "Must be greater than or equal to 100"  # <4>
        else:
            assert False, "the deposit should have been rejected"

    @Test
    def test_the_generated_constants_carry_the_name_and_the_expected_type(self):
        condition = RegistrationService_ELExpressions.REGISTER  # <3>
        assert condition.getExpectedType().getName() == "java.lang.Boolean", condition.getExpectedType()
        otherwise = RegistrationService_ELExpressions.REGISTER_OTHERWISE
        assert otherwise.getExpectedType().getName() == "java.lang.String", otherwise.getExpectedType()
