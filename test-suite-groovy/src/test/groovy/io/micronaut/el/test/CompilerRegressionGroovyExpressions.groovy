package io.micronaut.el.test

import io.micronaut.el.annotation.ELEnvironment
import io.micronaut.el.annotation.ELExpression
import io.micronaut.el.annotation.ELMethodExpression
import io.micronaut.el.annotation.ELVariable

/**
 * Regressions of the compiler that depend on the language the declaring class is written in: a primitive
 * class literal of an annotation, which the neutral metadata of Groovy drops, is still read. The interpreted
 * counterparts are in {@code io.micronaut.el.interpreter.CompilerRegressionInterpretedTest}.
 */
@ELEnvironment(variables = [
    @ELVariable(name = "text", type = CompilerRegressionGroovyText),
    @ELVariable(name = "count", type = int)
])
@ELExpression(value = '${count + 1}', name = "PRIMITIVE_VARIABLE")
@ELMethodExpression(value = '${text.repeat}', expectedReturnType = String,
    expectedParamTypes = [int], name = "PRIMITIVE_PARAMETER")
@ELMethodExpression(value = '${text.pad}', expectedReturnType = String,
    expectedParamTypes = [int, String], name = "PRIMITIVE_AND_REFERENCE_PARAMETERS")
class CompilerRegressionGroovyExpressions {
}

/**
 * The bean of the regressions, with overloads that only the primitive parameter types tell apart.
 */
class CompilerRegressionGroovyText {

    String repeat(int times) {
        "int:" + "ab" * times
    }

    String repeat(String times) {
        "string:" + times
    }

    String pad(int width, String fill) {
        "int:" + fill * width
    }

    String pad(long width, String fill) {
        "long:" + fill * width
    }
}
