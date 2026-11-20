package io.micronaut.el.test

import io.micronaut.el.CompiledELContext
import jakarta.el.ELContext
import jakarta.el.ExpressionFactory
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * The primitive class literals of an annotation declared in Groovy source reach the compiler: the parameter
 * types of a method expression keep their primitive types, so the overload they name is the one invoked, and
 * a variable declared with a primitive type compiles.
 */
class CompilerRegressionGroovyExpressionsSpec {

    private final ExpressionFactory factory = ExpressionFactory.newInstance()
    private final ELContext context = new CompiledELContext()
        .setBean("text", new CompilerRegressionGroovyText())
        .setBean("count", 2)

    @Test
    void aVariableOfAPrimitiveTypeCompiles() {
        assertEquals(3L, factory.createValueExpression(context, '${count + 1}', Object).getValue(context))
    }

    @Test
    void thePrimitiveParameterTypesOfAMethodExpressionAreKept() {
        // the compiled factory finds an expression by its text and its types, which only match if the
        // primitive survived; the interpreter is absent from this suite, so nothing else could answer
        def single = factory.createMethodExpression(context, '${text.repeat}', String, [int] as Class[])
        assertEquals("int:abab", single.invoke(context, [2] as Object[]))

        def mixed = factory.createMethodExpression(context, '${text.pad}', String, [int, String] as Class[])
        assertEquals("int:--", mixed.invoke(context, [2, "-"] as Object[]))
    }
}
