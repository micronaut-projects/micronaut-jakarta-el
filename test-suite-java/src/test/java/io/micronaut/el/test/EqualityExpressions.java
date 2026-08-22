package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELVariable;

@ELEnvironment(variables = @ELVariable(name = "book", type = Book.class))
@ELExpression(value = "${book.title}", expectedType = String.class, name = "TITLE")
@ELExpression(value = "#{ book.title }", expectedType = String.class, name = "TITLE_DEFERRED")
@ELExpression(value = "${book['title']}", expectedType = String.class, name = "TITLE_BRACKET")
@ELExpression(value = "${book.unitPrice lt 10}", expectedType = Boolean.class, name = "CHEAP")
@ELExpression(value = "${book.unitPrice < 10}", expectedType = Boolean.class, name = "CHEAP_SYMBOL")
@ELExpression(value = "${10 > book.unitPrice}", expectedType = Boolean.class, name = "CHEAP_REVERSED")
@ELMethodExpression(value = "${book.discounted(10)}", expectedReturnType = Double.class, name = "DISCOUNT_PARAMETERED")
@ELMethodExpression(value = "${book.discounted}", expectedReturnType = Double.class, expectedParamTypes = Double.class, name = "DISCOUNT")
public final class EqualityExpressions {
}
