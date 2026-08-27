package example;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.el.annotation.ELVariable;

import java.util.List;

@ELEnvironment(
    variables = {
        @ELVariable(name = "book", type = Book.class),
        @ELVariable(name = "books", type = List.class) // <1>
    },
    imports = Math.class, // <2>
    functions = @ELFunctions(value = TextFunctions.class, prefix = "text") // <3>
)
@ELExpression(value = "${text:shout(book.title)}", expectedType = String.class, name = "SHOUTED") // <4>
@ELExpression(value = "${text:initials(book.title)}", expectedType = String.class, name = "INITIALS")
@ELExpression(value = "${Math.max(book.unitPrice, 25.0)}", expectedType = double.class, name = "FLOOR_PRICE") // <5>
@ELExpression(
    value = "${books.stream().filter(b -> b.unitPrice > 10).map(b -> b.title).toList()}", // <6>
    expectedType = List.class,
    name = "EXPENSIVE_TITLES"
)
@ELExpression(expression = "${(price -> price * 2)(book.unitPrice)}", expectedType = double.class, name = "DOUBLED") // <7>
@ELExpression(
    expression = "${discount = (price, percent) -> price * (100 - percent) / 100; discount(book.unitPrice, 25)}", // <8>
    expectedType = double.class,
    name = "DISCOUNTED"
)
@ELExpression(
    expression = "${books.stream().sorted((a, b) -> a.unitPrice - b.unitPrice).map(b -> b.title).toList()}", // <9>
    expectedType = List.class,
    name = "BY_PRICE"
)
public class CatalogExpressions {
}
