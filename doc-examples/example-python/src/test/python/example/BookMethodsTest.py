import java
from java.util import List
from micronaut.el import CompiledELContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Disabled, Test

from example.Book import Book
from example.BookMethods import BookMethods

Object = java.type("java.lang.Object")
CompiledExpressionFactory = java.type("io.micronaut.el.CompiledExpressionFactory")
ContributedELMethodExecutor = java.type("io.micronaut.el.ContributedELMethodExecutor")
InterpretingELExpressionParser = java.type("io.micronaut.el.interpreter.InterpretingELExpressionParser")


@MicronautTest
class BookMethodsTest:

    def __init__(self):
        # TODO(python): the contributor is also named in META-INF/services/io.micronaut.el.ELMethodContributor, but the
        # service is loaded once per JVM and every Python test class runs in a GraalPy context of its own, so the
        # instance the service loader created in the context of an earlier test cannot run here: this test hands
        # the parser the contributor explicitly, the way the guide shows for leaving executors out.
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
    @Disabled("TODO(python): a Python class cannot declare the Java functional interface a lambda expression is coerced to")
    def test_a_lambda_reaches_the_application_interface_without_a_proxy(self):
        assert self.evaluate("${book.summarised(b -> b.title())}") == "Jakarta EL"
