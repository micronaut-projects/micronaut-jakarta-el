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
        assertEquals(25.0, factory.createValueExpression(context, "\${Math.max(book.unitPrice, 25.0)}", Double::class.java).getValue(context))
        assertEquals(listOf("Jakarta EL"), factory.createValueExpression(context,
            "\${books.stream().filter(b -> b.unitPrice > 10).map(b -> b.title).toList()}", List::class.java).getValue(context))
        assertEquals(40.0, factory.createValueExpression(context, "\${(price -> price * 2)(book.unitPrice)}", Double::class.java).getValue(context))
        assertEquals(15.0, factory.createValueExpression(context,
            "\${discount = (price, percent) -> price * (100 - percent) / 100; discount(book.unitPrice, 25)}", Double::class.java).getValue(context))
        assertEquals(listOf("Leaflet", "Jakarta EL"), factory.createValueExpression(context,
            "\${books.stream().sorted((a, b) -> a.unitPrice - b.unitPrice).map(b -> b.title).toList()}", List::class.java).getValue(context))
    }
}
