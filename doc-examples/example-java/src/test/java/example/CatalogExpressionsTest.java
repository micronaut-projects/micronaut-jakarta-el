package example;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogExpressionsTest {

    @Test
    void functionsImportsAndCollections() {
        ELContext context = new CompiledELContext()
            .setBean("book", new Book("Jakarta EL", "reference", 20d))
            .setBean("books", List.of(
                new Book("Jakarta EL", "reference", 20d),
                new Book("Leaflet", "marketing", 2d)));

        assertEquals("JAKARTA EL!", CatalogExpressions$ELExpressions.SHOUTED.getValue(context));
        assertEquals("JE", CatalogExpressions$ELExpressions.INITIALS.getValue(context));
        assertEquals(25d, CatalogExpressions$ELExpressions.FLOOR_PRICE.getValue(context));
        assertEquals(List.of("Jakarta EL"), CatalogExpressions$ELExpressions.EXPENSIVE_TITLES.getValue(context));
    }
}
