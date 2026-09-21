from java.lang import Object
from java.util import List
from micronaut.el import CompiledELContext, CompiledExpressionFactory, ContributedELMethodExecutor
from micronaut.el.interpreter import InterpretingELExpressionParser
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.Book import Book
from example.BookMethods import BookMethods


@MicronautTest
class BookMethodsTest:

    def __init__(self):
        # TODO(python): the contributor is also named in META-INF/services/io.micronaut.el.ELMethodContributor, but
        # ELContributions loads the services once per JVM and keeps the registrations: the lambdas BookMethods
        # registered belong to the GraalPy context of the first test class that touched the factory and are unusable
        # here ("Context execution was cancelled"), so this test hands the parser the contributor explicitly, the way
        # the guide shows for leaving executors out.
        self.factory = CompiledExpressionFactory(List.of(),
            InterpretingELExpressionParser(List.of(ContributedELMethodExecutor(List.of(BookMethods())))))
        self.context = CompiledELContext().setBean("book", Book("Jakarta EL", "reference", 20.0))

    def evaluate(self, expression: str):
        return self.factory.createValueExpression(self.context, expression, Object).getValue(self.context)

    @Test
    def test_the_contributed_methods_are_callable_from_an_expression_parsed_at_runtime(self):
        assert self.evaluate("${book.title()}") == "Jakarta EL"
        assert self.evaluate("${book.discounted(10)}") == 18.0
        assert self.evaluate("${book.label('$', 10)}") == "$18.0"
        assert self.evaluate("${book.tagged('-', 'a', 'b')}") == "Jakarta EL a-b"
        assert self.evaluate("${Math.abs(-7)}") == 7
        assert self.evaluate("${fmt:shout('hi')}") == "HI!"

    @Test
    def test_a_lambda_reaches_the_application_interface_without_a_proxy(self):
        assert self.evaluate("${book.summarised(b -> b.title())}") == "Jakarta EL"
