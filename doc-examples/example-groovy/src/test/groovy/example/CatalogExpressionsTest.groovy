package example

import io.micronaut.el.CompiledELContext
import jakarta.el.ELContext
import jakarta.el.ELManager
import jakarta.el.ExpressionFactory
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class CatalogExpressionsTest {

    @Test
    void functionsImportsAndCollections() {
        ELContext context = new CompiledELContext()
            .setBean("book", new Book("Jakarta EL", "reference", 20d))
            .setBean("books", [new Book("Jakarta EL", "reference", 20d), new Book("Leaflet", "marketing", 2d)])
        ExpressionFactory factory = ELManager.expressionFactory

        assertEquals("JAKARTA EL!", factory.createValueExpression(context, '${text:shout(book.title)}', String).getValue(context))
        assertEquals("JE", factory.createValueExpression(context, '${text:initials(book.title)}', String).getValue(context))
        assertEquals(25d, factory.createValueExpression(context, '${Math.max(book.unitPrice, 25)}', double).getValue(context))
        assertEquals(["Jakarta EL"], factory.createValueExpression(context,
            '${books.stream().filter(b -> b.unitPrice > 10).map(b -> b.title).toList()}', List).getValue(context))
    }
}
