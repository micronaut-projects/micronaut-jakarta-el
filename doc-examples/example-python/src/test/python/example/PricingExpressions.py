from micronaut.el.annotation import ELEnvironment, ELExpression, ELVariable

from example.Book import Book


@ELEnvironment(variables=[ELVariable(name="book", type=Book)])  # <1>
@ELExpression(value="${pricing:quote(book, 3)}", expectedType=float, name="QUOTE")  # <2>
@ELExpression(value="${pricing:quote(book, 3) += ' ' += pricing:currency()}", expectedType=str, name="PRICED")  # <3>
class PricingExpressions:
    pass
