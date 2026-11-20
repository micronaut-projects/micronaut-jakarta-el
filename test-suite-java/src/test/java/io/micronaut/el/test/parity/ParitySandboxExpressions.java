package io.micronaut.el.test.parity;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

import java.util.List;
import java.util.Map;

/**
 * The closest compile-time equivalents of {@code ELSandboxTest}. The sandbox is applied by the interpreter to
 * the expression strings an application builds at runtime; a compiled expression is source of the application and
 * is not sandboxed. So only the regressions asserting that the sandbox leaves the language alone have a generated
 * counterpart with the same result: an expression the sandbox denies would evaluate when compiled, by design.
 */
@ELEnvironment(variables = {
    @ELVariable(name = "bean", type = String.class),
    @ELVariable(name = "list", type = List.class),
    @ELVariable(name = "map", type = Map.class)
})
// theLanguageItselfIsUntouched
@ELExpression(value = "${bean.toUpperCase()}", name = "upperCase")
@ELExpression(value = "${bean.length()}", name = "length")
@ELExpression(value = "${Integer.valueOf('7')}", name = "integerValueOf")
@ELExpression(value = "${Math.max(1,2)}", name = "maximum")
@ELExpression(value = "${list.stream().map(x -> x * 2).toList()}", name = "doubled")
@ELExpression(value = "${list.stream().sum()}", name = "sum")
@ELExpression(value = "${map['k']}", name = "mapEntry")
@ELExpression(value = "${list[0]}", name = "firstElement")
@ELExpression(value = "${x = 3; x * 2}", name = "assignment")
// theSandboxOfTheContextReplacesTheStandardOne, aCompiledExpressionIsNotSandboxed
@ELExpression(value = "${bean.getClass().getName()}", name = "className")
public final class ParitySandboxExpressions {

    private ParitySandboxExpressions() {
    }
}
