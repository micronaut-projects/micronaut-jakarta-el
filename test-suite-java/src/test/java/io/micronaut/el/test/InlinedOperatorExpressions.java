package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

/**
 * The operators applied to operands whose primitive types are known at compilation time.
 */
@ELEnvironment(variables = @ELVariable(name = "item", type = Inventory.class))
@ELExpression(value = "${item.quantity + 1}", name = "addLiteral")
@ELExpression(value = "${item.quantity + item.reorderLevel}", name = "addIntegral")
@ELExpression(value = "${item.quantity * item.price}", name = "multiplyMixed")
@ELExpression(value = "${item.price - item.weight}", name = "subtractFloating")
@ELExpression(value = "${item.quantity / 2}", name = "divideIntegral")
@ELExpression(value = "${item.quantity mod 2}", name = "moduloIntegral")
@ELExpression(value = "${item.price mod 2}", name = "moduloFloating")
@ELExpression(value = "${-item.quantity}", name = "negateInt")
@ELExpression(value = "${-item.reorderLevel}", name = "negateLong")
@ELExpression(value = "${-item.price}", name = "negateDouble")
@ELExpression(value = "${-item.weight}", name = "negateFloat")
@ELExpression(value = "${item.quantity > 3}", name = "greaterThan")
@ELExpression(value = "${item.quantity <= item.reorderLevel}", name = "lessThanOrEqual")
@ELExpression(value = "${item.quantity == 5}", name = "equalIntegral")
@ELExpression(value = "${item.quantity != 5}", name = "notEqualIntegral")
@ELExpression(value = "${item.price >= 9.99}", name = "greaterThanOrEqualFloating")
@ELExpression(value = "${item.weight < item.price}", name = "lessThanMixed")
@ELExpression(value = "${item.quantity == 5.0}", name = "equalMixed")
@ELExpression(value = "${item.nan > 1}", name = "nanGreaterThan")
@ELExpression(value = "${item.nan < 1}", name = "nanLessThan")
@ELExpression(value = "${item.nan == item.nan}", name = "nanEqual")
@ELExpression(value = "${item.sku == 'A-1'}", name = "stringEqual")
@ELExpression(value = "${item.sku != 'A-1'}", name = "stringNotEqual")
@ELExpression(value = "${item.available == true}", name = "booleanEqual")
@ELExpression(value = "${not item.available}", name = "not")
@ELExpression(value = "${item.available and item.quantity > 0}", name = "and")
@ELExpression(value = "${item.available or item.quantity > 0}", name = "or")
@ELExpression(value = "${item.available ? 'in stock' : 'sold out'}", name = "ternary")
@ELExpression(value = "${item.sku += ' x' += item.quantity}", name = "concat")
@ELExpression(value = "SKU ${item.sku} qty ${item.quantity} suit ${item.suit} weight ${item.weight} ok ${item.available}", name = "composite")
public final class InlinedOperatorExpressions {

    private InlinedOperatorExpressions() {
    }
}
