import java
from jakarta.el import ELManager
from micronaut.el import CompiledELContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.Book import Book

CompiledExpression = java.type("io.micronaut.el.runtime.CompiledExpression")
String = java.type("java.lang.String")
Double = java.type("java.lang.Double")
BookExpressions_ELExpressions = java.type("example.BookExpressions$ELExpressions")


@MicronautTest
class BookExpressionsTest:

    @Test
    def test_evaluates_the_compiled_expressions(self):
        context = CompiledELContext().setBean("book", Book("Jakarta EL", "reference", 20.0))  # <1>
        factory = ELManager.getExpressionFactory()

        title = factory.createValueExpression(context, "${book.title}", String)  # <2>
        assert isinstance(title, CompiledExpression)  # <3>
        assert title.getValue(context) == "Jakarta EL"
        assert factory.createValueExpression(context, "Book: ${book.title} at ${book.unitPrice}", String).getValue(context) == "Book: Jakarta EL at 20.0"
        assert factory.createMethodExpression(context, "${book.discounted(10)}", Double, []).invoke(context, None) == 18.0  # <4>

        assert BookExpressions_ELExpressions.PRICE_BAND.getValue(context) == "expensive"  # <5>
