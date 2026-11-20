package io.micronaut.el.test

import io.micronaut.el.annotation.ELEnvironment
import io.micronaut.el.annotation.ELExpression
import io.micronaut.el.annotation.ELMethodExpression
import io.micronaut.el.annotation.ELVariable

/**
 * Regressions of the compiler that depend on the language the declaring class is written in: a primitive
 * class literal of an annotation (`Int::class` is `int.class` to an annotation) is still read. The interpreted
 * counterparts are in `io.micronaut.el.interpreter.CompilerRegressionInterpretedTest`.
 */
@ELEnvironment(variables = [
    ELVariable(name = "text", type = CompilerRegressionKotlinText::class),
    ELVariable(name = "count", type = Int::class)
])
@ELExpression(value = "\${count + 1}", name = "PRIMITIVE_VARIABLE")
@ELMethodExpression(value = "\${text.repeat}", expectedReturnType = String::class,
    expectedParamTypes = [Int::class], name = "PRIMITIVE_PARAMETER")
@ELMethodExpression(value = "\${text.pad}", expectedReturnType = String::class,
    expectedParamTypes = [Int::class, String::class], name = "PRIMITIVE_AND_REFERENCE_PARAMETERS")
class CompilerRegressionKotlinExpressions

/**
 * The bean of the regressions, with overloads that only the primitive parameter types tell apart.
 */
class CompilerRegressionKotlinText {

    fun repeat(times: Int): String = "int:" + "ab".repeat(times)

    fun repeat(times: String): String = "string:$times"

    fun pad(width: Int, fill: String): String = "int:" + fill.repeat(width)

    fun pad(width: Long, fill: String): String = "long:" + fill.repeat(width.toInt())
}
