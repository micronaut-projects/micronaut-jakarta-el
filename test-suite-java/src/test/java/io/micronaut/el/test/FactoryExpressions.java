package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

@ELEnvironment(variables = @ELVariable(name = "suit", type = Suit.class))
@ELExpression(value = "${suit == 'SPADE'}", expectedType = Boolean.class, name = "isSpade")
@ELExpression(value = "Welcome ${customer} to our site", expectedType = String.class, name = "welcome")
@ELExpression(value = "${customer}", expectedType = String.class, name = "customer")
public final class FactoryExpressions {

    private FactoryExpressions() {
    }
}
