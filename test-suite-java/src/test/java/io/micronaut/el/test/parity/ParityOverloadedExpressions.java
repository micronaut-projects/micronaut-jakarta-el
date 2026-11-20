package io.micronaut.el.test.parity;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

/**
 * The compile-time counterpart of {@code ELMethodContributorTest.anOverloadIsSelectedAsTheSpecificationRequires},
 * whose receiver declares both {@code greet(String)} and {@code greet(Integer)}.
 */
@ELEnvironment(variables = @ELVariable(name = "greeter", type = ParityOverloadedGreeter.class))
@ELExpression(value = "${greeter.greet(2)}", name = "integerOverload")
public final class ParityOverloadedExpressions {

    private ParityOverloadedExpressions() {
    }
}
