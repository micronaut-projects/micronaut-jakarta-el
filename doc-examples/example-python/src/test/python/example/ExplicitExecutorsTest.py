from java.lang import Object
from java.util import List
from jakarta.el import MethodNotFoundException
from micronaut.el import CompiledELContext, CompiledExpressionFactory, ContributedELMethodExecutor
from micronaut.el.interpreter import InterpretingELExpressionParser
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.Book import Book
from example.BookMethods import BookMethods


@MicronautTest
class ExplicitExecutorsTest:

    @Test
    def test_only_the_contributed_methods_are_reachable(self):
        factory = CompiledExpressionFactory(List.of(),  # <1>
            InterpretingELExpressionParser(List.of(ContributedELMethodExecutor(List.of(BookMethods())))))  # <2>
        context = CompiledELContext().setBean("book", Book("Jakarta EL", "reference", 20.0))

        assert factory.createValueExpression(context, "${book.title()}", Object).getValue(context) == "Jakarta EL"  # <3>
        try:
            factory.createValueExpression(context, "${book.getTitle()}", Object).getValue(context)  # <4>
        except MethodNotFoundException:
            pass
        else:
            assert False, "the method should not have been found"
