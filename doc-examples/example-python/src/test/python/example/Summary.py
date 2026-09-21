from abc import ABC, abstractmethod

from example.Book import Book


class Summary(ABC):
    """A functional interface of the application, which an expression can pass a lambda to."""

    @abstractmethod
    def of(self, book: Book) -> str:
        ...
