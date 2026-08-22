package io.micronaut.el.test

import io.micronaut.el.CompiledELContext
import io.micronaut.el.resolver.IntrospectionELResolver
import jakarta.el.ELProcessor
import jakarta.el.ExpressionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Uses the module from Kotlin. The bean is introspected at compilation time by Micronaut, so its properties and
 * its executable methods resolve without reflection; the expressions are parsed by the interpreter module.
 */
class BookExpressionsTest {

    private val factory: ExpressionFactory = ExpressionFactory.newInstance()
    private val context = CompiledELContext().setBean("book", Book("Expression Language", "reference", 20.0))

    @Test
    fun `evaluates expressions over a Kotlin bean`() {
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
            factory.createValueExpression(context, "\${book.unitPrice > 15 ? 'expensive' : 'cheap'}", String::class.java)
                .getValue(context)
        )
    }

    @Test
    fun `resolves the introspected Kotlin bean without reflection`() {
        val resolver = IntrospectionELResolver()
        context.setPropertyResolved(false)
        assertEquals("Expression Language", resolver.getValue(context, context.getBean("book"), "title"))
        assertTrue(context.isPropertyResolved)
    }

    @Test
    fun `supports the collection operations of the specification`() {
        val processor = ELProcessor()
        processor.defineBean(
            "books",
            listOf(
                Book("a", "history", 30.0),
                Book("b", "history", 10.0),
                Book("c", "science", 20.0)
            )
        )
        assertEquals(
            listOf("a", "c"),
            processor.eval("books.stream().filter(b -> b.unitPrice >= 20).map(b -> b.title).toList()")
        )
        assertEquals(60.0, processor.eval("books.stream().map(b -> b.unitPrice).sum()"))
    }
}
