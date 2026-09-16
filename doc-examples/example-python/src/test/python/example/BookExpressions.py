from micronaut.el.annotation import ELEnvironment, ELExpression, ELMethodExpression, ELVariable

from example.Book import Book


@ELEnvironment(variables=[ELVariable(name="book", type=Book)])  # <1>
@ELExpression(value="${book.title}", expectedType=str, name="TITLE")  # <2>
@ELExpression(value="Book: ${book.title} at ${book.unitPrice}", expectedType=str, name="SUMMARY")  # <3>
@ELExpression(value="${book.unitPrice > 15 ? 'expensive' : 'cheap'}", expectedType=str, name="PRICE_BAND")
@ELMethodExpression(value="${book.discounted(10)}", expectedReturnType=float, name="DISCOUNTED")  # <4>
class BookExpressions:
    pass
