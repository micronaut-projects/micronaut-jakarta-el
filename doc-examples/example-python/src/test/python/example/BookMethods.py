import java
from java.lang import Double, Long, Math, String
from micronaut.el import ELMethodContributor, ELMethodRegistry

from example.TextFunctions import TextFunctions

from example.Book import Book

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
            .staticMethod(Math, "abs", long, Long, lambda value: Math.abs(value))  # <7>
            .constructor(Book, [String, String, double], False,
                         lambda context, base, arguments: Book(arguments[0], arguments[1], arguments[2]))  # <8>
            .function("fmt", "shout", TextFunctions, "shout", String, String,
                      lambda text: TextFunctions.shout(text)))  # <9>
        # TODO(python): a Python class cannot declare a Java interface, so the `summarised` method taking the
        # `Summary` functional interface of the application, and the `functionalInterface` registration saying how
        # a lambda expression becomes one (callouts 6 and 10 of the Java example), are not registered:
        # `functionalInterface` requires an interface, and the class generated for the Python `Summary` is not one.

    def getOrder(self) -> int:  # <11>
        return -100
