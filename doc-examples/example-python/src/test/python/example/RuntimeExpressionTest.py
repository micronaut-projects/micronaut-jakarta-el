import java
from jakarta.el import ELManager
from micronaut.el import CompiledELContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.Book import Book

CompiledExpression = java.type("io.micronaut.el.runtime.CompiledExpression")
String = java.type("java.lang.String")


@MicronautTest
class RuntimeExpressionTest:

    @Test
    def test_parses_an_expression_built_at_runtime(self):
        context = CompiledELContext().setBean("book", Book("Jakarta EL", "reference", 20.0))
        property = "category"  # <1>

        expression = ELManager.getExpressionFactory() \
            .createValueExpression(context, "${book." + property + "}", String)  # <2>

        assert not isinstance(expression, CompiledExpression)  # <3>
        assert expression.getValue(context) == "reference"
