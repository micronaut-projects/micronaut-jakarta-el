package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.LambdaExpression;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CatalogExpressionsTest {

    private final List<Book> books = List.of(
        new Book("Expression Language", "history", 20d),
        new Book("Sourcegen", "computing", 5d)
    );
    private final Author author = new Author("Denis", books);
    private final CompiledELContext context = new CompiledELContext()
        .setBean("author", author)
        .setBean("books", books)
        .setBean("strings", new String[]{"a", "b"})
        .setBean("customer", author);

    @Test
    void staticallyTypedResolution() {
        assertEquals("Denis", value(CatalogExpressions$ELExpressions.AUTHOR_NAME));
        assertEquals("Hi, Denis", value(CatalogExpressions$ELExpressions.GREETING));
        assertEquals("Expression Language", value(CatalogExpressions$ELExpressions.FIRST_TITLE));
    }

    @Test
    void dynamicResolutionUsesTheGeneratedResolvers() {
        assertEquals("Denis", value(CatalogExpressions$ELExpressions.DYNAMIC_NAME));
        assertEquals("history", value(CatalogExpressions$ELExpressions.DYNAMIC_CATEGORY));
    }

    @Test
    void functions() {
        assertEquals(5, value(CatalogExpressions$ELExpressions.NAME_LENGTH));
        assertEquals("DENIS", value(CatalogExpressions$ELExpressions.UPPER_NAME));
        assertEquals("a,b", value(CatalogExpressions$ELExpressions.FUNCTION_JOIN));
        assertEquals("a,b", value(CatalogExpressions$ELExpressions.FUNCTION_ARRAY_JOIN));
        assertEquals("a,b", value(CatalogExpressions$ELExpressions.STATIC_JOIN));
    }

    @Test
    void staticReferencesAndConstructors() {
        assertEquals(Suit.SPADE, value(CatalogExpressions$ELExpressions.SUIT));
        assertEquals(Suit.SPADE, value(CatalogExpressions$ELExpressions.IMPORTED_SUIT));
        assertEquals(Boolean.TRUE, value(CatalogExpressions$ELExpressions.BOOLEAN_CONSTANT));
        assertEquals(42, value(CatalogExpressions$ELExpressions.STATIC_METHOD));
        assertEquals("EL", ((Book) value(CatalogExpressions$ELExpressions.NEW_BOOK)).getTitle());
        assertEquals("a,b", value(CatalogExpressions$ELExpressions.VARARGS_CONSTRUCTOR));
        assertEquals("a,b", value(CatalogExpressions$ELExpressions.VARARGS));
    }

    @Test
    void collectionOperations() {
        assertEquals(List.of("Expression Language"), value(CatalogExpressions$ELExpressions.EXPENSIVE));
        assertEquals(25d, value(CatalogExpressions$ELExpressions.TOTAL));
        assertEquals("Expression Language", value(CatalogExpressions$ELExpressions.MOST_EXPENSIVE));
    }

    @Test
    void lambdaExpressions() {
        assertEquals(7L, value(CatalogExpressions$ELExpressions.IMMEDIATE_LAMBDA));
        assertEquals(7L, value(CatalogExpressions$ELExpressions.ASSIGNED_LAMBDA));
        assertEquals(120L, value(CatalogExpressions$ELExpressions.FACTORIAL));
        LambdaExpression outer = (LambdaExpression) value(CatalogExpressions$ELExpressions.NESTED_LAMBDA);
        LambdaExpression inner = (LambdaExpression) outer.invoke(context, 3L);
        assertEquals(7L, inner.invoke(context, 4L));
    }

    private Object value(ValueExpression expression) {
        return expression.getValue(context);
    }
}
