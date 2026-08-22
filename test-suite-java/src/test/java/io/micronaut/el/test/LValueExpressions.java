package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELVariable;

@ELEnvironment(variables = {
    @ELVariable(name = "book", type = Book.class),
    @ELVariable(name = "author", type = Author.class)
})
@ELExpression(value = "${book.title}", expectedType = String.class, name = "title")
@ELExpression(value = "${book.category}", expectedType = String.class, name = "category")
@ELExpression(value = "${counter}", name = "counter")
@ELExpression(value = "${1 + 1}", name = "notAnLValue")
@ELMethodExpression(value = "${book.describe}", expectedReturnType = String.class, name = "describe")
@ELMethodExpression(value = "${book.discounted(50)}", expectedReturnType = Double.class, name = "halfPrice")
@ELMethodExpression(
    value = "${author.greet}",
    expectedReturnType = String.class,
    expectedParamTypes = String.class,
    name = "greet"
)
public final class LValueExpressions {

    private LValueExpressions() {
    }
}
