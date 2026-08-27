package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELVariable;

import java.util.List;

@ELEnvironment(variables = {
    @ELVariable(name = "book", type = Book.class),
    @ELVariable(name = "author", type = Author.class),
    @ELVariable(name = "formatting", type = Formatting.class),
    @ELVariable(name = "xs", type = List.class)
})
@ELExpression(value = "${book.title}", expectedType = String.class, name = "title")
@ELExpression(value = "${book.category}", expectedType = String.class, name = "category")
@ELExpression(value = "${counter}", expectedType = Object.class, name = "counter")
@ELExpression(value = "${1 + 1}", name = "notAnLValue")
@ELMethodExpression(value = "${book.describe}", expectedReturnType = String.class, name = "describe")
@ELMethodExpression(value = "${book.discounted(50)}", expectedReturnType = Double.class, name = "halfPrice")
@ELMethodExpression(
    value = "${author.greet}",
    expectedReturnType = String.class,
    expectedParamTypes = String.class,
    name = "greet"
)
@ELMethodExpression(
    value = "${formatting.select}",
    expectedReturnType = String.class,
    expectedParamTypes = Integer.class,
    name = "selectInteger"
)
@ELMethodExpression(
    value = "${formatting.select}",
    expectedReturnType = String.class,
    expectedParamTypes = String.class,
    name = "selectString"
)
@ELMethodExpression(value = "${Integer.valueOf}", expectedReturnType = Integer.class,
    expectedParamTypes = String.class, name = "integerValueOf")
@ELMethodExpression(value = "${xs.size}", expectedReturnType = Integer.class,
    name = "listSize")
@ELExpression(value = "${formatting.argumentType(formatting.numbers)}", expectedType = String.class, name = "packedPrimitiveArray")
public final class LValueExpressions {

    private LValueExpressions() {
    }
}
