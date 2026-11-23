package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

/**
 * An array passed as the single argument of a variable arity parameter {@code T...}, to which it is assignable,
 * is the array of the parameter, not its one element: section 1.2.1.2 of the specification follows section
 * 15.12.2 of the JLS, where such an invocation is applicable in the first phase. Every kind of invocation spreads
 * it alike. The interpreted counterparts, with the same text and results, are in
 * {@code io.micronaut.el.interpreter.VarargsSpreadInterpretedTest}.
 *
 * <p>Unlike {@code RuntimeVarargsSpreadExpressions}, whose receivers are undeclared and resolved at runtime, the
 * receiver here is declared, so the compiler selects the instance and static methods statically; the JDK static
 * method, the function and the constructor that both would share are only in {@code RuntimeVarargsSpreadExpressions}.</p>
 */
@ELEnvironment(
    variables = {
        @ELVariable(name = "bean", type = VarargsSpreadBean.class),
        @ELVariable(name = "strings", type = String[].class),
        @ELVariable(name = "objects", type = Object[].class),
        @ELVariable(name = "numbers", type = int[].class)
    },
    imports = VarargsSpreadBean.class,
    staticImports = VarargsSpreadFunctions.class
)
// an instance method, alone and after a fixed parameter
@ELExpression(value = "${bean.argumentType(strings)}", expectedType = Object.class, name = "instanceSubtypeArray")
@ELExpression(value = "${bean.join('-', strings)}", expectedType = Object.class, name = "instanceJoin")
// a static method, through its class and through an instance
@ELExpression(value = "${VarargsSpreadBean.staticCount(strings)}", expectedType = Object.class, name = "staticThroughClass")
@ELExpression(value = "${bean.staticCount(strings)}", expectedType = Object.class, name = "staticThroughInstance")
// a statically imported method
@ELExpression(value = "${count(strings)}", expectedType = Object.class, name = "staticImport")
// an array of the very type of the parameter, and one of a primitive component, which is not assignable to Object[]
@ELExpression(value = "${bean.argumentType(objects)}", expectedType = Object.class, name = "sameTypeArray")
@ELExpression(value = "${bean.argumentType(numbers)}", expectedType = Object.class, name = "primitiveArray")
public final class VarargsSpreadExpressions {

    private VarargsSpreadExpressions() {
    }
}
