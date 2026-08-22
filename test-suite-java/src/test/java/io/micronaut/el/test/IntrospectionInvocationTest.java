package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.MethodNotFoundException;
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
    void anArrayGivenDirectlyIsPassedThroughNotWrapped() {
        assertEquals("a-b", invoke("join", "-", new Object[]{new Object[]{"a", "b"}}));
        assertEquals(3, invoke("size", new Object[]{new Object[]{1, 2, 3}}));
    }

    @Test
    void aFixedArityArrayParameterIsNotPackedFromAScalar() {
        // size(Object[]) with a scalar is not a fit for the introspection; the chain is left to decide
        context.setPropertyResolved(false);
        assertThrows(MethodNotFoundException.class, () -> invoke("size", "x"));
    }

    @Test
    void theResolverDeclinesRatherThanCommittingToAMismatch() {
        io.micronaut.el.resolver.IntrospectionELResolver alone = new io.micronaut.el.resolver.IntrospectionELResolver();
        context.setPropertyResolved(false);
        assertNull(alone.invoke(context, formatting, "size", null, new Object[]{"x"}));
        assertFalse(context.isPropertyResolved());
    }

    @Test
    void theCompiledAndInterpretedPathsSeeTheSameResult() {
        jakarta.el.ExpressionFactory factory = jakarta.el.ExpressionFactory.newInstance();
        assertEquals("0.50", factory.createValueExpression(context, "${f.format('%.2f', 0.5)}", String.class).getValue(context));
    }
}
