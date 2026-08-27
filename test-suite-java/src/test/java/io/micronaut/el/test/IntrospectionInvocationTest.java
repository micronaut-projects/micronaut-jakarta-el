package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.MethodNotFoundException;
import io.micronaut.el.resolver.IntrospectionELResolver;
import jakarta.el.ELException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The invocation of executable methods through the bean introspection, including the variable arity ones.
 */
class IntrospectionInvocationTest {

    private final ELContext context = new CompiledELContext().setBean("f", new Formatting());
    private final ELResolver resolver = context.getELResolver();
    private final Formatting formatting = new Formatting();

    private Object invoke(String method, Object... arguments) {
        context.setPropertyResolved(false);
        return resolver.invoke(context, formatting, method, null, arguments);
    }

    @Test
    void packsTheTrailingArgumentsOfAVariableArityMethod() {
        assertEquals("a-b", invoke("join", "-", "a", "b"));
        assertEquals("a", invoke("join", "-", "a"));
        assertEquals("0.50", invoke("format", "%.2f", 0.5d));
        assertEquals("1 and 2", invoke("format", "%s and %s", 1L, 2L));
        assertTrue(context.isPropertyResolved());
    }

    @Test
    void theFixedArityOverloadWinsWhenItFits() {
        assertEquals("only:-", invoke("join", "-"));
    }

    @Test
    void equallySpecificOverloadsAreAmbiguous() {
        assertThrows(MethodNotFoundException.class, () -> invoke("ambiguous", 1));
    }

    @Test
    void equallyCoercibleOverloadsAreAmbiguous() {
        assertThrows(MethodNotFoundException.class, () -> invoke("ambiguousCoercion", "1"));
    }

    @Test
    void anArrayGivenDirectlyIsPassedThroughNotWrapped() {
        assertEquals("a-b", invoke("join", "-", new Object[]{"a", "b"}));
        assertEquals(3, invoke("size", new Object[]{new Object[]{1, 2, 3}}));
    }

    @Test
    void aScalarIsPackedIntoATrailingArrayParameter() {
        // a BeanMethod does not say whether the array is variadic, so a trailing array parameter is treated as
        // one: a scalar that would otherwise fail to coerce to the array is packed into it
        assertEquals(1, invoke("size", "x"));
    }

    @Test
    void theResolverDeclinesRatherThanCommittingToAMismatch() {
        IntrospectionELResolver alone = new IntrospectionELResolver();
        context.setPropertyResolved(false);
        assertNull(alone.invoke(context, formatting, "twice", null, new Object[]{"not a number"}));
        assertFalse(context.isPropertyResolved());
        // the standard resolvers then get their chance, and report the mismatch themselves
        assertThrows(ELException.class, () -> invoke("twice", "not a number"));
        assertEquals(6, invoke("twice", "3"));
    }

    @Test
    void theCompiledExpressionsReachTheFormatterThroughTheChain() {
        assertEquals("0.50", FormattingExpressions$ELExpressions.FORMATTED.getValue(context));
        assertEquals("a-b-c", FormattingExpressions$ELExpressions.JOINED.getValue(context));
        assertEquals("only:-", FormattingExpressions$ELExpressions.JOINED_ALONE.getValue(context));
    }
}
