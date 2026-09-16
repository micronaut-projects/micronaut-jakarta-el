import java
from micronaut.el import CompiledELContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.Book import Book

CatalogExpressions_ELExpressions = java.type("example.CatalogExpressions$ELExpressions")


@MicronautTest
class CatalogExpressionsTest:

    @Test
    def test_functions_imports_and_collections(self):
        context = CompiledELContext() \
            .setBean("book", Book("Jakarta EL", "reference", 20.0)) \
            .setBean("books", [
                Book("Jakarta EL", "reference", 20.0),
                Book("Leaflet", "marketing", 2.0)])

        assert CatalogExpressions_ELExpressions.SHOUTED.getValue(context) == "JAKARTA EL!"
        assert CatalogExpressions_ELExpressions.INITIALS.getValue(context) == "JE"
        assert CatalogExpressions_ELExpressions.FLOOR_PRICE.getValue(context) == 25.0
        assert list(CatalogExpressions_ELExpressions.EXPENSIVE_TITLES.getValue(context)) == ["Jakarta EL"]
        assert CatalogExpressions_ELExpressions.DOUBLED.getValue(context) == 40.0
        assert CatalogExpressions_ELExpressions.DISCOUNTED.getValue(context) == 15.0
        assert list(CatalogExpressions_ELExpressions.BY_PRICE.getValue(context)) == ["Leaflet", "Jakarta EL"]
