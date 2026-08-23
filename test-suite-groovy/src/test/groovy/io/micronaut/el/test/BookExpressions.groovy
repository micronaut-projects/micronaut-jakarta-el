package io.micronaut.el.test

import io.micronaut.el.annotation.ELEnvironment
import io.micronaut.el.annotation.ELExpression
import io.micronaut.el.annotation.ELVariable

@ELEnvironment(variables = @ELVariable(name = "book", type = Book))
@ELExpression(value = '${book.title}', expectedType = String, name = "TITLE")
@ELExpression(value = 'Book: ${book.title} at ${book.unitPrice}', expectedType = String, name = "SUMMARY")
@ELExpression(value = '${book.discounted(10)}', expectedType = Double, name = "DISCOUNTED")
@ELExpression(value = '${book.unitPrice > 15 ? "expensive" : "cheap"}', expectedType = String, name = "PRICE_BAND")
@ELExpression(value = '${book.unitPrice * 2 + 1}', name = "DOUBLED")
@ELExpression(value = '${-book.unitPrice}', name = "NEGATED")
@ELExpression(value = '${not (book.unitPrice > 15) or book.title == "Expression Language"}', name = "LOGICAL")
@ELExpression(value = '${book.title += ": " += book.unitPrice}', name = "CONCATENATED")
@ELExpression(value = '${book.tags.stream().filter(t -> t.length() > 1).map(t -> t += "!").toList()}', name = "STREAM")
@ELExpression(value = '${book.count(t -> t.length() > 1)}', name = "FUNCTIONAL")
@ELExpression(value = '${(x -> y -> x + y)(1)(2)}', name = "NESTED_LAMBDA")
@ELExpression(value = '${((a, b, c, d) -> a + b + c + d)(1, 2, 3, 4)}', name = "FOUR_PARAMETERS")
@ELExpression(value = '${book.tags.stream().forEach(t -> t.length())}', name = "FOR_EACH")
class BookExpressions {
}
