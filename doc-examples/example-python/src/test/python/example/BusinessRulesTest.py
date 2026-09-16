from typing import Annotated

import java
from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.BusinessRules import BusinessRules
from example.Customer import Customer

# TODO(python): the exception raised by the Python interceptor reaches the caller as the Java class generated for it
NotEligibleException = java.type("example.NotEligibleException")


@MicronautTest
class BusinessRulesTest:

    rules: Annotated[BusinessRules, Inject]

    @Test
    def test_the_rules_guard_the_methods(self):
        assert self.rules.register(Customer("Ann", 34, "CZ")) == "registered Ann"
        try:
            self.rules.register(Customer("Bob", 15, "CZ"))
        except NotEligibleException as rejected:
            assert rejected.getMessage() == "Bob must be an adult in Europe"
        else:
            assert False, "the registration should have been rejected"

        assert self.rules.deposit(101) == "deposited 101"
        try:
            self.rules.deposit(100)
        except NotEligibleException as too_small:
            assert too_small.getMessage() == "Must be greater than 100"
        else:
            assert False, "the deposit should have been rejected"
