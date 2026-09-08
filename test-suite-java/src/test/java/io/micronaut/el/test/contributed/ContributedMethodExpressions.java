package io.micronaut.el.test.contributed;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.el.annotation.ELVariable;

/**
 * Compile-time counterparts of the interpreted regressions of
 * {@code io.micronaut.el.interpreter.ELMethodContributorTest}, with the same expression text and the same
 * expected results.
 *
 * <p>What a contributor declares to the interpreter — an instance method of variable arity, a static method, a
 * constructor and a function — the compiler resolves from the declared types instead, so both execution modes
 * evaluate the same expressions.</p>
 */
@ELEnvironment(
    variables = @ELVariable(name = "greeter", type = Greeter.class),
    imports = Greeter.class,
    functions = @ELFunctions(prefix = "greet", value = Greetings.class)
)
@ELExpression(value = "${greeter.greet('world')}", name = "greet")
@ELExpression(value = "${greeter.join('-', 'a', 'b', 'c')}", name = "join")
@ELExpression(value = "${greeter.join('-')}", name = "joinWithoutParts")
@ELExpression(value = "${Math.abs(-7)}", name = "staticAbsolute")
@ELExpression(value = "${Greeter('ada').name}", name = "constructed")
@ELExpression(value = "${greet:twice('ab')}", name = "twice")
public final class ContributedMethodExpressions {

    private ContributedMethodExpressions() {
    }
}
