package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELVariable;

/**
 * Expressions declared without an expected type: the compiler infers it from the static type of the expression.
 */
@ELEnvironment(variables = @ELVariable(name = "item", type = Inventory.class))
@ELExpression(value = "${item.price}", name = "price")
@ELExpression(value = "${item.quantity > 3}", name = "inStock")
@ELExpression(value = "${item.sku}", name = "sku")
@ELExpression(value = "${item.tags}", name = "tags")
@ELExpression(value = "${item.suit}", name = "suit")
@ELMethodExpression(value = "${item.count(t -> t.length() > 1)}", name = "count")
public final class InferredTypeExpressions {

    private InferredTypeExpressions() {
    }
}
