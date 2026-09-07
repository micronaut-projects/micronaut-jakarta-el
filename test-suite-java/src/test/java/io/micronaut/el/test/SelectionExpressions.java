package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELVariable;

/**
 * Compile-time counterparts of the selection and coercion regressions of the reflective and contributed
 * dispatch, with the same expression text and the same expected results.
 */
@ELEnvironment(
    variables = @ELVariable(name = "selection", type = Selection.class),
    imports = Constructed.class
)
@ELMethodExpression(value = "${Math.max}", expectedReturnType = Object.class,
    expectedParamTypes = {int.class, int.class}, name = "mathMax")
@ELMethodExpression(expression = "${Math.min}", expectedReturnType = Object.class,
    expectedParamTypes = {int.class, int.class}, name = "mathMin")
@ELMethodExpression(value = "${Math.max}", expectedReturnType = Object.class,
    expectedParamTypes = {long.class, long.class}, name = "mathMaxLong")
@ELExpression(value = "${selection.nullable(null)}", name = "nullOverload")
@ELExpression(value = "${Constructed(1).selected}", name = "constructorSpecificity")
@ELExpression(value = "${selection.sorted((a, b) -> null)}", name = "comparatorNull")
public final class SelectionExpressions {

    private SelectionExpressions() {
    }
}
