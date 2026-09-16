from typing import Annotated

import java
from jakarta.el import ELException
from jakarta.inject import Inject
from micronaut.context import ApplicationContext
from micronaut.el import CompiledELContext, ELBeanProvider
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.Book import Book
from example.PricingService import PricingService

PricingExpressions_ELExpressions = java.type("example.PricingExpressions$ELExpressions")
ELBeanProviderType = java.type("io.micronaut.el.ELBeanProvider")
PricingServiceType = java.type("example.PricingService")


class ContextBeanProvider(ELBeanProvider):
    """Serves the beans of the application context to the expressions."""

    def __init__(self, application_context: ApplicationContext):
        self.application_context = application_context

    def get(self, type):
        return self.application_context.getBean(type)


@MicronautTest
class PricingExpressionsTest:

    application_context: Annotated[ApplicationContext, Inject]

    def __init__(self):
        self.book = Book("Jakarta EL", "reference", 20.0)

    @Test
    def test_the_function_is_invoked_on_the_bean(self):
        context = CompiledELContext().setBean("book", self.book)
        context.putContext(ELBeanProviderType, ContextBeanProvider(self.application_context))  # <1>

        assert PricingExpressions_ELExpressions.QUOTE.getValue(context) == 54.0  # <2>
        assert PricingExpressions_ELExpressions.PRICED.getValue(context) == "54.0 EUR"

    @Test
    def test_an_instance_can_be_registered_directly(self):
        context = CompiledELContext().setBean("book", self.book)
        context.putContext(PricingServiceType, PricingService())  # <3>

        assert PricingExpressions_ELExpressions.QUOTE.getValue(context) == 54.0

    @Test
    def test_without_an_instance_the_evaluation_fails(self):
        context = CompiledELContext().setBean("book", self.book)

        try:
            PricingExpressions_ELExpressions.QUOTE.getValue(context)  # <4>
        except ELException as failure:
            assert "No instance of example.PricingService" in failure.getMessage(), failure.getMessage()
        else:
            assert False, "the evaluation should have failed"
