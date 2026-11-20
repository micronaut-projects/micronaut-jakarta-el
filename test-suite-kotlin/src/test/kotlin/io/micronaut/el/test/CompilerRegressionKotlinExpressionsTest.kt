package io.micronaut.el.test

import io.micronaut.el.CompiledELContext
import jakarta.el.ExpressionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The primitive class literals of an annotation declared in Kotlin source reach the compiler: the parameter
 * types of a method expression keep their primitive types, so the compiled factory finds the expression by them
 * and the overload they name is the one invoked, and a variable declared with a primitive type compiles. The
 * interpreter is absent from this suite, so nothing else could answer.
 */
class CompilerRegressionKotlinExpressionsTest {

    private val factory: ExpressionFactory = ExpressionFactory.newInstance()
    private val context = CompiledELContext()
        .setBean("text", CompilerRegressionKotlinText())
        .setBean("count", 2)

    @Test
    fun `a variable of a primitive type compiles`() {
        assertEquals(3L, factory.createValueExpression(context, "\${count + 1}", Any::class.java).getValue(context))
    }

    @Test
    fun `the primitive parameter types of a method expression are kept`() {
        val single = factory.createMethodExpression(context, "\${text.repeat}", String::class.java,
            arrayOf<Class<*>>(Int::class.javaPrimitiveType!!))
        assertEquals("int:abab", single.invoke(context, arrayOf<Any>(2)))
        val mixed = factory.createMethodExpression(context, "\${text.pad}", String::class.java,
            arrayOf<Class<*>>(Int::class.javaPrimitiveType!!, String::class.java))
        assertEquals("int:--", mixed.invoke(context, arrayOf<Any>(2, "-")))
    }
}
