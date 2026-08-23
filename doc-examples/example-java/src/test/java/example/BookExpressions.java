package example;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELVariable;

@ELEnvironment(variables = @ELVariable(name = "book", type = Book.class)) // <1>
@ELExpression(value = "${book.title}", expectedType = String.class, name = "TITLE") // <2>
@ELExpression(value = "Book: ${book.title} at ${book.unitPrice}", expectedType = String.class, name = "SUMMARY") // <3>
@ELExpression(value = "${book.unitPrice > 15 ? 'expensive' : 'cheap'}", expectedType = String.class, name = "PRICE_BAND")
@ELMethodExpression(value = "${book.discounted(10)}", expectedReturnType = double.class, name = "DISCOUNTED") // <4>
public class BookExpressions {
}
