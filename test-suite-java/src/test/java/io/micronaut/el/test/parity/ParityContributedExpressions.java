package io.micronaut.el.test.parity;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELVariable;

/**
 * Compile-time counterparts of the expressions of {@code ELMethodContributorTest}. A contributed executor makes a
 * method callable from an interpreted expression without reflection; a compiled expression calls the same method
 * directly, so these assert that both reach the same method with the same arguments.
 *
 * <p>Left out, because each is a contract of the contributed registry that a compiled expression has no
 * equivalent of:</p>
 * <ul>
 *     <li>{@code ${greeter.count()}} is not found because no contributor registered {@code count}; a compiled
 *     expression calls any method the static type declares, and fails compilation for one it does not;</li>
 *     <li>{@code ${text.describe()}} calls a method registered on {@link CharSequence} that {@link String} does
 *     not declare, so it fails compilation;</li>
 *     <li>{@code ${greeter.greet('a', 'b')}} fits no overload, so it fails compilation;</li>
 *     <li>{@code ${LoudGreeter('ada')}} is not found because the registered constructor of the supertype must
 *     not construct the subtype; a compiled expression calls the constructor the type declares.</li>
 * </ul>
 * <p>The interpreted runtime contract of each stays in {@code ELMethodContributorTest}.</p>
 */
@ELEnvironment(variables = {
    @ELVariable(name = "greeter", type = ParityGreeter.class),
    @ELVariable(name = "impl", type = ParityDiamond.class)
})
@ELExpression(value = "${greeter.greet(42)}", name = "coercedArgument")
@ELMethodExpression(value = "${greeter.greet}", expectedReturnType = String.class,
    expectedParamTypes = String.class, name = "greetMethod")
@ELExpression(value = "${greeter.name}", name = "name")
@ELExpression(value = "${greeter.pick(1)}", name = "overriddenPick")
@ELExpression(value = "${greeter.getName()}", name = "inheritedGetName")
@ELExpression(value = "${impl.pick(1)}", name = "nearestPick")
public final class ParityContributedExpressions {

    private ParityContributedExpressions() {
    }
}
