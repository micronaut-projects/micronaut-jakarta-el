package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELVariable;
import jakarta.el.LambdaExpression;

@ELEnvironment(variables = {
    @ELVariable(name = "suit", type = Suit.class),
    @ELVariable(name = "action", type = LambdaExpression.class)
})
@ELExpression(value = "${suit == 'SPADE'}", expectedType = Boolean.class, name = "isSpade")
@ELExpression(value = "Welcome ${customer} to our site", expectedType = String.class, name = "welcome")
@ELExpression(value = "${customer}", expectedType = String.class, name = "customer")
@ELMethodExpression(value = "${action}", expectedReturnType = String.class,
    expectedParamTypes = String.class, name = "action")
public final class FactoryExpressions {

    private FactoryExpressions() {
    }
}
