package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.resolver.CompiledBeanELResolver;
import io.micronaut.el.resolver.ELBeanResolver;
import jakarta.el.ELResolver;
import jakarta.el.MethodNotFoundException;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.PropertyNotWritableException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedResolverTest {

    private final ELBeanResolver resolver = new Book$ELResolver();
    private final CompiledELContext context = new CompiledELContext();
    private final Book book = new Book("EL", "history", 20d);

    @Test
    void theResolverDescribesTheBean() {
        assertEquals(Book.class, resolver.getBeanType());
        assertEquals(Set.of("title", "category", "unitPrice"), resolver.getPropertyNames());
        assertEquals(String.class, resolver.getPropertyType("title"));
        assertEquals(double.class, resolver.getPropertyType("unitPrice"));
        assertNull(resolver.getPropertyType("missing"));
        assertFalse(resolver.isReadOnly("title"));
        assertTrue(resolver.isReadOnly("category"));
        assertTrue(resolver.isReadOnly("missing"));
    }

    @Test
    void theResolverReadsAndWritesProperties() {
        assertEquals("EL", resolver.getProperty(context, book, "title"));
        resolver.setProperty(context, book, "title", "Sourcegen");
        assertEquals("Sourcegen", book.getTitle());
        resolver.setProperty(context, book, "unitPrice", "12.5");
        assertEquals(12.5d, book.getUnitPrice());
        assertThrows(PropertyNotFoundException.class, () -> resolver.getProperty(context, book, "missing"));
        assertThrows(PropertyNotWritableException.class,
            () -> resolver.setProperty(context, book, "category", "computing"));
    }

    @Test
    void theResolverInvokesMethods() {
        assertTrue(resolver.hasMethod("describe", 0));
        assertTrue(resolver.hasMethod("discounted", 1));
        assertFalse(resolver.hasMethod("discounted", 2));
        assertFalse(resolver.hasMethod("missing", 0));
        assertEquals("EL (history)", resolver.invokeMethod(context, book, "describe", new Object[0]));
        assertEquals(18d, resolver.invokeMethod(context, book, "discounted", new Object[]{"10"}));
        assertThrows(MethodNotFoundException.class,
            () -> resolver.invokeMethod(context, book, "missing", new Object[0]));
    }

    @Test
    void theResolverIsRegisteredAsAService() {
        ELResolver chain = new CompiledBeanELResolver();
        context.setPropertyResolved(false);
        assertEquals("EL", chain.getValue(context, book, "title"));
        assertTrue(context.isPropertyResolved());
    }

    @Test
    void theStandardResolversStillApply() {
        CompiledELContext elContext = new CompiledELContext()
            .setBean("map", java.util.Map.of("a", 1))
            .setBean("list", List.of("x", "y"))
            .setBean("array", new String[]{"x", "y"});
        ELResolver chain = elContext.getELResolver();
        elContext.setPropertyResolved(false);
        assertEquals(1, chain.getValue(elContext, java.util.Map.of("a", 1), "a"));
        elContext.setPropertyResolved(false);
        assertEquals("y", chain.getValue(elContext, List.of("x", "y"), 1));
        elContext.setPropertyResolved(false);
        assertEquals(2, chain.getValue(elContext, new String[]{"x", "y"}, "length"));
    }
}
