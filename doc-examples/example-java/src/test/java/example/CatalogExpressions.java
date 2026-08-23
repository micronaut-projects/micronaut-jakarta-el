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
@ELExpression(value = "${Math.max(book.unitPrice, 25)}", expectedType = double.class, name = "FLOOR_PRICE") // <5>
@ELExpression(
    value = "${books.stream().filter(b -> b.unitPrice > 10).map(b -> b.title).toList()}", // <6>
    expectedType = List.class,
    name = "EXPENSIVE_TITLES"
)
public class CatalogExpressions {
}
