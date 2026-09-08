package io.micronaut.el.test

import io.micronaut.el.CompiledELContext
import io.micronaut.el.resolver.IntrospectionELResolver
import io.micronaut.el.runtime.CompiledExpression
import jakarta.el.ELException
import jakarta.el.ExpressionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
    fun `evaluates the operators inlined from the primitive properties`() {
        assertEquals(41.0, factory.createValueExpression(context, "\${book.unitPrice * 2 + 1}", Any::class.java).getValue(context))
        assertEquals(-20.0, factory.createValueExpression(context, "\${-book.unitPrice}", Any::class.java).getValue(context))
        assertEquals(
            true,
            factory.createValueExpression(
                context,
                "\${not (book.unitPrice > 15) or book.title == 'Expression Language'}",
                Any::class.java
            ).getValue(context)
        )
        assertEquals(
            "Expression Language: 20.0",
            factory.createValueExpression(context, "\${book.title += ': ' += book.unitPrice}", Any::class.java).getValue(context)
        )
    }

    @Test
    fun `evaluates the lambda expressions compiled to Java lambdas`() {
        assertEquals(
            listOf("new!", "sale!"),
            factory.createValueExpression(context, "\${book.tags.stream().filter(t -> t.length() > 1).map(t -> t += '!').toList()}", Any::class.java).getValue(context)
        )
        assertEquals(2L, factory.createValueExpression(context, "\${book.count(t -> t.length() > 1)}", Any::class.java).getValue(context))
        assertEquals(3L, factory.createValueExpression(context, "\${(x -> y -> x + y)(1)(2)}", Any::class.java).getValue(context))
        assertEquals(10L, factory.createValueExpression(context, "\${((a, b, c, d) -> a + b + c + d)(1, 2, 3, 4)}", Any::class.java).getValue(context))
        assertNull(factory.createValueExpression(context, "\${book.tags.stream().forEach(t -> t.length())}", Any::class.java).getValue<Any?>(context))
    }

    @Test
    fun `resolves the lvalue protocol of a compiled expression`() {
        // the expressions of this suite are written as bytecode rather than as Java source, so the calls the
        // generated methods make are the ones the writer emitted, with no compiler of the language to bind
        // them: getValueReference builds a jakarta.el.ValueReference from the base and the property
        val expression = factory.createValueExpression(context, "\${book.title}", String::class.java)
        assertFalse(expression.isReadOnly(context))
        assertEquals(String::class.java, expression.getType(context))
        val reference = expression.getValueReference(context)
        assertEquals(context.getBean("book"), reference.base)
        assertEquals("title", reference.property)
        expression.setValue(context, "Sourcegen")
        assertEquals("Sourcegen", expression.getValue(context))
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
