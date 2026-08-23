package example

import io.micronaut.el.annotation.ELEnvironment
import io.micronaut.el.annotation.ELExpression
import io.micronaut.el.annotation.ELFunctions
import io.micronaut.el.annotation.ELVariable

@ELEnvironment(
    variables = [
        @ELVariable(name = "book", type = Book),
        @ELVariable(name = "books", type = List) // <1>
    ],
    imports = Math, // <2>
    functions = @ELFunctions(value = TextFunctions, prefix = "text") // <3>
)
@ELExpression(value = '${text:shout(book.title)}', expectedType = String, name = "SHOUTED") // <4>
@ELExpression(value = '${text:initials(book.title)}', expectedType = String, name = "INITIALS")
@ELExpression(value = '${Math.max(book.unitPrice, 25)}', expectedType = double, name = "FLOOR_PRICE") // <5>
@ELExpression(
    value = '${books.stream().filter(b -> b.unitPrice > 10).map(b -> b.title).toList()}', // <6>
    expectedType = List,
    name = "EXPENSIVE_TITLES"
)
class CatalogExpressions {
}
