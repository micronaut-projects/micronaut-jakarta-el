package example

import io.micronaut.el.CompiledELContext
import jakarta.el.ELManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CatalogExpressionsTest {

    @Test
    fun functionsImportsAndCollections() {
        val context = CompiledELContext()
            .setBean("book", Book("Jakarta EL", "reference", 20.0))
            .setBean("books", listOf(Book("Jakarta EL", "reference", 20.0), Book("Leaflet", "marketing", 2.0)))
        val factory = ELManager.getExpressionFactory()

        assertEquals("JAKARTA EL!", factory.createValueExpression(context, "\${text:shout(book.title)}", String::class.java).getValue(context))
        assertEquals("JE", factory.createValueExpression(context, "\${text:initials(book.title)}", String::class.java).getValue(context))
        assertEquals(25.0, factory.createValueExpression(context, "\${Math.max(book.unitPrice, 25)}", Double::class.java).getValue(context))
        assertEquals(listOf("Jakarta EL"), factory.createValueExpression(context,
            "\${books.stream().filter(b -> b.unitPrice > 10).map(b -> b.title).toList()}", List::class.java).getValue(context))
    }
}
