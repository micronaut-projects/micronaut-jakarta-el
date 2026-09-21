from jakarta.inject import Singleton
from micronaut.context.annotation import Executable
from micronaut.el.annotation import ELFunction

from example.Book import Book


@Singleton  # <1>
class PricingService:

    discount: float = 0.1

    @Executable
    @ELFunction(prefix="pricing")  # <2>
    def quote(self, book: Book, quantity: int) -> float:
        return book.unitPrice * quantity * (1 - self.discount)

    @staticmethod
    @Executable
    @ELFunction(prefix="pricing", name="currency")  # <3>
    def currencyCode() -> str:
        return "EUR"
