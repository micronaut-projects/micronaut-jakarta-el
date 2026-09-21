import java
from java.lang import Double, Long, Math, String
from micronaut.el import ELMethodContributor, ELMethodRegistry

from example.Book import Book
from example.Summary import Summary
from example.TextFunctions import TextFunctions

# TODO(python): the primitive and array class literals have no import form
StringArray = java.type("java.lang.String[]")
double = java.type("double")
long = java.type("long")


class BookMethods(ELMethodContributor):  # <1>

    def contribute(self, registry: ELMethodRegistry) -> None:
        (registry
            .method(Book, "title", String, lambda book: book.title)  # <2>
            .method(Book, "discounted", double, Double, lambda book, percent: book.discounted(percent))  # <3>
            .method(Book, "label", String, String, Double,
                    lambda book, currency, percent: currency + str(book.discounted(percent)))  # <4>
            .method(Book, "tagged", String, [String, StringArray], True,
                    lambda context, base, arguments: base.title + " " + arguments[0].join(arguments[1]))  # <5>
            .method(Book, "summarised", String, Summary, lambda book, summary: summary.of(book))  # <6>
            .staticMethod(Math, "abs", long, Long, lambda value: Math.abs(value))  # <7>
            .constructor(Book, [String, String, double], False,
                         lambda context, base, arguments: Book(arguments[0], arguments[1], arguments[2]))  # <8>
            .function("fmt", "shout", TextFunctions, "shout", String, String,
                      lambda text: TextFunctions.shout(text))  # <9>
            .functionalInterface(Summary,
                                 lambda context, lambda_: LambdaSummary(context, lambda_)))  # <10>

    def getOrder(self) -> int:  # <11>
        return -100


class LambdaSummary(Summary):
    """The Summary a lambda expression becomes: invokes the lambda with the book."""

    def __init__(self, context, lambda_):
        self.context = context
        self.lambda_ = lambda_

    def of(self, book: Book) -> str:
        return str(self.lambda_.invoke(self.context, book))
