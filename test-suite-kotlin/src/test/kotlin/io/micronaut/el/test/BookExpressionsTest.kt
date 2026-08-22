package io.micronaut.el.test

import io.micronaut.el.CompiledELContext
import io.micronaut.el.resolver.IntrospectionELResolver
import io.micronaut.el.runtime.CompiledExpression
import jakarta.el.ELException
import jakarta.el.ExpressionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The expressions of this suite are declared in Kotlin source and compiled by the annotation processor running
 * under KSP. The interpreter is deliberately absent from the classpath, so an expression that reaches the runtime
 * unparsed fails rather than falling back.
 */
class BookExpressionsTest {

    private val factory: ExpressionFactory = ExpressionFactory.newInstance()
    private val context = CompiledELContext().setBean("book", Book("Expression Language", "reference", 20.0))

    @Test
    fun `evaluates the expressions compiled from Kotlin source`() {
        assertEquals(
            "Expression Language",
            factory.createValueExpression(context, "\${book.title}", String::class.java).getValue(context)
        )
        assertEquals(
            "Book: Expression Language at 20.0",
            factory.createValueExpression(context, "Book: \${book.title} at \${book.unitPrice}", String::class.java)
                .getValue(context)
        )
        assertEquals(
            "expensive",
            factory.createValueExpression(
                context,
                "\${book.unitPrice > 15 ? 'expensive' : 'cheap'}",
                String::class.java
            ).getValue(context)
        )
    }

    @Test
    fun `the expressions are compiled rather than parsed`() {
        assertTrue(factory.createValueExpression(context, "\${book.title}", String::class.java) is CompiledExpression)
    }

    @Test
    fun `an undeclared expression is rejected without the interpreter`() {
        assertThrows(ELException::class.java) {
            factory.createValueExpression(context, "\${book.category}", String::class.java)
        }
    }

    @Test
    fun `resolves the introspected Kotlin bean without reflection`() {
        val resolver = IntrospectionELResolver()
        context.setPropertyResolved(false)
        assertEquals("Expression Language", resolver.getValue(context, context.getBean("book"), "title"))
        assertTrue(context.isPropertyResolved)
    }
}
