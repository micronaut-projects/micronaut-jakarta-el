package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

/**
 * A variable declared with a primitive type selects overloads as its value, an object, does: the interpreter,
 * Expressly and Tomcat pass an {@code Integer} and select {@code which(Integer)} over {@code which(int)}, see
 * {@code InterpreterRegressionDifferentialTest.aPrimitiveValueSelectsTheWrapperOverload}. The value is still read
 * and coerced as a primitive where the parameter is one, a null becoming zero, and is otherwise the object it is: a
 * null compares to null and is empty. The interpreted counterparts, with
 * the same text and results, are in {@code io.micronaut.el.interpreter.PrimitiveVariableInterpretedTest}.
 */
@ELEnvironment(variables = {
    @ELVariable(name = "bean", type = PrimitiveVariableBean.class),
    @ELVariable(name = "count", type = int.class),
    @ELVariable(name = "none", type = int.class)
})
@ELExpression(value = "${bean.which(count)}", expectedType = Object.class, name = "variable")
@ELExpression(value = "${bean.which(bean.count)}", expectedType = Object.class, name = "property")
@ELExpression(value = "${bean.which(count + 0)}", expectedType = Object.class, name = "arithmetic")
@ELExpression(value = "${bean.repeat(count)}", expectedType = Object.class, name = "primitiveParameter")
@ELExpression(value = "${bean.repeat(none)}", expectedType = Object.class, name = "nullToPrimitive")
@ELExpression(value = "${count + 1}", expectedType = Object.class, name = "primitiveArithmetic")
@ELExpression(value = "${none + 1}", expectedType = Object.class, name = "nullArithmetic")
@ELExpression(value = "${none == null}", expectedType = Object.class, name = "nullComparison")
@ELExpression(value = "${empty none}", expectedType = Object.class, name = "nullEmpty")
public final class PrimitiveVariableExpressions {

    private PrimitiveVariableExpressions() {
    }
}
