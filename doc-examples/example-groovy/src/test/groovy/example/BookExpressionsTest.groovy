package example

import io.micronaut.el.CompiledELContext
import io.micronaut.el.runtime.CompiledExpression
import jakarta.el.ELContext
import jakarta.el.ELManager
import jakarta.el.ExpressionFactory
import jakarta.el.ValueExpression
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class BookExpressionsTest {

    @Test
    void evaluatesTheCompiledExpressions() {
        ELContext context = new CompiledELContext() // <1>
            .setBean("book", new Book("Jakarta EL", "reference", 20d))
        ExpressionFactory factory = ELManager.expressionFactory

        ValueExpression title = factory.createValueExpression(context, '${book.title}', String) // <2>
        assertTrue(title instanceof CompiledExpression) // <3>
        assertEquals("Jakarta EL", title.getValue(context))
        assertEquals("Book: Jakarta EL at 20.0",
            factory.createValueExpression(context, 'Book: ${book.title} at ${book.unitPrice}', String).getValue(context))
        assertEquals(18d,
            factory.createMethodExpression(context, '${book.discounted(10)}', double, new Class[0]).invoke(context, null)) // <4>

        assertEquals("expensive",
            factory.createValueExpression(context, "\${book.unitPrice > 15 ? 'expensive' : 'cheap'}", String).getValue(context))
    }
}
