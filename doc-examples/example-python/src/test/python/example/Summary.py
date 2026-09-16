class Summary:
    """A functional interface of the application, which an expression can pass a lambda to."""

    def of(self, book) -> str:
        ...
