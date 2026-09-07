package io.micronaut.el.test.executable;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELMethodExpression;

/**
 * Compile-time counterparts of the interpreted regressions of
 * {@code io.micronaut.el.interpreter.executable.ExecutableMethodInterpreterTest}, with the same expression
 * text and the same expected results.
 *
 * <p>None of the bases is declared as a variable, so the compiler emits a dynamic resolution and the
 * expressions go through the resolver chain, the way an expression naming a bean of an application does.</p>
 */
@ELEnvironment
@ELExpression(value = "${greeter.greet('world')}", expectedType = Object.class, name = "greet")
@ELExpression(value = "${greeter.twice('21')}", expectedType = Object.class, name = "twice")
@ELExpression(value = "${greeter.select('1')}", expectedType = Object.class, name = "select")
@ELExpression(value = "${greeter.ambiguous(1)}", expectedType = Object.class, name = "ambiguous")
@ELExpression(value = "${greeter.hidden('world')}", expectedType = Object.class, name = "hidden")
@ELExpression(value = "${introspected.greet('world')}", expectedType = Object.class, name = "introspected")
@ELExpression(value = "${advised.greet('world')}", expectedType = Object.class, name = "advised")
@ELExpression(value = "${plain.shout('world')}", expectedType = Object.class, name = "plain")
@ELMethodExpression(value = "${greeter.greet('world')}", expectedReturnType = String.class, name = "greetMethod")
public final class ExecutableMethodExpressions {

    private ExecutableMethodExpressions() {
    }
}
