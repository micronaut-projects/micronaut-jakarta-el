package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

@ELEnvironment(variables = @ELVariable(name = "book", type = Book.class))
@ELExpression(value = "${book.title}", expectedType = String.class, name = "title")
@ELExpression(value = "Book: ${book.title} costs ${book.unitPrice}", expectedType = String.class, name = "summary")
@ELExpression(value = "${book.discounted(10)}", expectedType = Double.class, name = "discounted")
public final class BookExpressions {

    private BookExpressions() {
    }
}
