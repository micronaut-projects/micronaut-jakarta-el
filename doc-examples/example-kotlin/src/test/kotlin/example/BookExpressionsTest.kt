package example

import io.micronaut.el.CompiledELContext
import io.micronaut.el.runtime.CompiledExpression
import jakarta.el.ELManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BookExpressionsTest {

    @Test
    fun evaluatesTheCompiledExpressions() {
        val context = CompiledELContext() // <1>
            .setBean("book", Book("Jakarta EL", "reference", 20.0))
        val factory = ELManager.getExpressionFactory()

        val title = factory.createValueExpression(context, "\${book.title}", String::class.java) // <2>
        assertTrue(title is CompiledExpression) // <3>
        assertEquals("Jakarta EL", title.getValue(context))
        assertEquals("Book: Jakarta EL at 20.0",
            factory.createValueExpression(context, "Book: \${book.title} at \${book.unitPrice}", String::class.java).getValue(context))
        assertEquals(18.0,
            factory.createMethodExpression(context, "\${book.discounted(10)}", Double::class.java, arrayOf()).invoke(context, null)) // <4>

        assertEquals("expensive", `BookExpressions$ELExpressions`.PRICE_BAND.getValue(context)) // <5>
    }
}
