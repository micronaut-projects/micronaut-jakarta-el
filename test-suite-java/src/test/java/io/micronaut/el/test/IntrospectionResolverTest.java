package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.resolver.ELResolvers;
import io.micronaut.el.resolver.IntrospectionELResolver;
import jakarta.el.ELResolver;
import jakarta.el.MapELResolver;
import jakarta.el.PropertyNotWritableException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntrospectionResolverTest {

    private final ELResolver resolver = new IntrospectionELResolver();
    private final CompiledELContext context = new CompiledELContext();
    private final Book book = new Book("EL", "history", 20d);

    @Test
    void readsPropertiesThroughTheIntrospection() {
        context.setPropertyResolved(false);
        assertEquals("EL", resolver.getValue(context, book, "title"));
        assertTrue(context.isPropertyResolved());

        context.setPropertyResolved(false);
        assertEquals(20d, resolver.getValue(context, book, "unitPrice"));
        assertTrue(context.isPropertyResolved());
    }

    @Test
    void writesPropertiesThroughTheIntrospection() {
        context.setPropertyResolved(false);
        resolver.setValue(context, book, "title", "Expression Language");
        assertTrue(context.isPropertyResolved());
        assertEquals("Expression Language", book.getTitle());
    }

    @Test
    void reportsTheTypeAndTheMutabilityOfAProperty() {
        assertEquals(String.class, resolver.getType(context, book, "title"));
        assertFalse(resolver.isReadOnly(context, book, "title"));
        // category has no setter
        assertTrue(resolver.isReadOnly(context, book, "category"));
        assertThrows(PropertyNotWritableException.class, () -> resolver.setValue(context, book, "category", "x"));
    }

    @Test
    void invokesExecutableMethodsThroughTheIntrospection() {
        context.setPropertyResolved(false);
        assertEquals(18d, resolver.invoke(context, book, "discounted", null, new Object[]{10d}));
        assertTrue(context.isPropertyResolved());
    }

    @Test
    void leavesUnknownPropertiesAndTypesToTheRestOfTheChain() {
        context.setPropertyResolved(false);
        assertNull(resolver.getValue(context, book, "missing"));
        assertFalse(context.isPropertyResolved());

        context.setPropertyResolved(false);
        assertNull(resolver.getValue(context, "not introspected", "length"));
        assertFalse(context.isPropertyResolved());
    }

    @Test
    void theStandardChainResolvesIntrospectedBeansFirst() {
        ELResolver chain = ELResolvers.standard();
        context.setPropertyResolved(false);
        assertEquals("history", chain.getValue(context, book, "category"));
        assertTrue(context.isPropertyResolved());
    }

    @Test
    void customResolversRemainAheadOfTheIntrospectionResolver() {
        ELResolver custom = new MapELResolver();

        List<ELResolver> resolvers = ELResolvers.standardResolvers(custom);

        assertSame(custom, resolvers.get(0));
        assertInstanceOf(IntrospectionELResolver.class, resolvers.get(1));

        CompiledELContext customContext = new CompiledELContext(custom);
        customContext.setPropertyResolved(false);
        assertEquals("history", customContext.getELResolver().getValue(customContext, book, "category"));
        assertTrue(customContext.isPropertyResolved());
    }

    @Test
    void eachStandardResolverChainIsIsolated() {
        assertNotSame(ELResolvers.standard(), ELResolvers.standard());

        List<ELResolver> first = ELResolvers.standardResolvers();
        List<ELResolver> second = ELResolvers.standardResolvers();
        assertInstanceOf(IntrospectionELResolver.class, first.get(0));
        assertInstanceOf(IntrospectionELResolver.class, second.get(0));
        assertNotSame(first.get(0), second.get(0));
    }
}
