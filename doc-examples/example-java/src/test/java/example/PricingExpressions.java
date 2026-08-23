package example;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.el.annotation.ELVariable;

@ELEnvironment(
    variables = @ELVariable(name = "book", type = Book.class),
    functions = @ELFunctions(PricingService.class) // <1>
)
@ELExpression(value = "${pricing:quote(book, 3)}", expectedType = double.class, name = "QUOTE") // <2>
@ELExpression(value = "${pricing:quote(book, 3) += ' ' += pricing:currency()}", expectedType = String.class, name = "PRICED") // <3>
public class PricingExpressions {
}
