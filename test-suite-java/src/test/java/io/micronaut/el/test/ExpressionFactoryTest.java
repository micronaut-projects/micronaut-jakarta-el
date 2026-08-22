package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpressionFactoryTest {

    private final ExpressionFactory factory = ExpressionFactory.newInstance();
    private final CompiledELContext context = new CompiledELContext()
        .setBean("suit", Suit.SPADE)
        .setBean("customer", "Guy Lafleur");

    @Test
    void theCompiledFactoryIsRegisteredAsAService() {
        assertEquals("io.micronaut.el.CompiledExpressionFactory", factory.getClass().getName());
    }

    @Test
    void compiledExpressionsAreResolvedByTheirString() {
        ValueExpression expression = factory.createValueExpression(context, "${suit == 'SPADE'}", Boolean.class);
        assertEquals(FactoryExpressions$ELExpressions.IS_SPADE, expression);
        assertEquals(Boolean.TRUE, expression.getValue(context));
    }

    @Test
    void compositeExpressions() {
        assertEquals("Welcome Guy Lafleur to our site",
            FactoryExpressions$ELExpressions.WELCOME.getValue(context));
        assertEquals("Guy Lafleur", FactoryExpressions$ELExpressions.CUSTOMER.getValue(context));
    }

    @Test
    void literalExpressionsDoNotNeedToBeCompiled() {
        ValueExpression literal = factory.createValueExpression(context, "Aloha!", String.class);
        assertTrue(literal.isLiteralText());
        assertEquals("Aloha!", literal.getValue(context));

        ValueExpression escaped = factory.createValueExpression(context, "\\${exprA}", String.class);
        assertEquals("${exprA}", escaped.getValue(context));

        ValueExpression bool = factory.createValueExpression(context, "true", Boolean.class);
        assertEquals(Boolean.TRUE, bool.getValue(context));
    }

    @Test
    void anExpressionThatWasNotCompiledIsRejected() {
        ELException exception = assertThrows(ELException.class,
            () -> factory.createValueExpression(context, "${somethingElse}", String.class));
        assertTrue(exception.getMessage().contains("@ELExpression"));
    }

    @Test
    void valuesAreCoercedToTheExpectedType() {
        assertEquals(42, factory.coerceToType("42", Integer.class));
        assertEquals(Suit.SPADE, factory.coerceToType("SPADE", Suit.class));
    }

    @Test
    void objectValueExpressions() {
        ValueExpression expression = factory.createValueExpression(Suit.HEART, Suit.class);
        assertEquals(Suit.HEART, expression.getValue(context));
    }

    @Test
    void theStreamResolverIsProvided() {
        assertNotNull(factory.getStreamELResolver());
    }
}
