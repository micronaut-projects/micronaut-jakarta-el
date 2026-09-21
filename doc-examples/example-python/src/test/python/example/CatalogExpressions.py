from java.lang import Math
from java.util import List
from micronaut.el.annotation import ELEnvironment, ELExpression, ELFunctions, ELVariable

from example.Book import Book
from example.TextFunctions import TextFunctions


@ELEnvironment(
    variables=[
        ELVariable(name="book", type=Book),
        ELVariable(name="books", type=List),  # <1>
    ],
    imports=[Math],  # <2>
    functions=[ELFunctions(value=TextFunctions, prefix="text")],  # <3>
)
@ELExpression(value="${text:shout(book.title)}", expectedType=str, name="SHOUTED")  # <4>
@ELExpression(value="${text:initials(book.title)}", expectedType=str, name="INITIALS")
@ELExpression(value="${Math.max(book.unitPrice, 25.0)}", expectedType=float, name="FLOOR_PRICE")  # <5>
@ELExpression(
    value="${books.stream().filter(b -> b.unitPrice > 10).map(b -> b.title).toList()}",  # <6>
    expectedType=List,
    name="EXPENSIVE_TITLES",
)
@ELExpression(expression="${(price -> price * 2)(book.unitPrice)}", expectedType=float, name="DOUBLED")  # <7>
@ELExpression(
    expression="${discount = (price, percent) -> price * (100 - percent) / 100; discount(book.unitPrice, 25)}",  # <8>
    expectedType=float,
    name="DISCOUNTED",
)
@ELExpression(
    expression="${books.stream().sorted((a, b) -> a.unitPrice - b.unitPrice).map(b -> b.title).toList()}",  # <9>
    expectedType=List,
    name="BY_PRICE",
)
class CatalogExpressions:
    pass
