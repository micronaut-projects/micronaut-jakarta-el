package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.ELExpressionSource;
import io.micronaut.el.runtime.CompiledExpression;
import io.micronaut.el.runtime.ELLambdas;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.LambdaExpression;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        assertTrue(expression instanceof CompiledExpression);
        assertEquals(FactoryExpressions$ELExpressions.IS_SPADE.getExpressionString(), expression.getExpressionString());
        assertEquals(FactoryExpressions$ELExpressions.IS_SPADE.getExpectedType(), expression.getExpectedType());
        assertEquals(Boolean.TRUE, expression.getValue(context));
    }

    @Test
    void theGeneratedSourceDeclaresItsExpressions() {
        ELExpressionSource source = new FactoryExpressions$ELExpressions();

        assertTrue(source.expressions().contains("${suit == 'SPADE'}"),
            () -> "Expected the source to declare its expressions but got " + source.expressions());
        assertEquals(source.expressions().size(), source.expressions().stream().distinct().count());
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
    void compiledVariablesAreBoundWhenTheExpressionIsCreated() {
        context.getVariableMapper().setVariable("customer", factory.createValueExpression("first", String.class));
        ValueExpression expression = factory.createValueExpression(context, "${customer}", String.class);

        context.getVariableMapper().setVariable("customer", factory.createValueExpression("second", String.class));

        assertEquals("first", expression.getValue(context));
    }

    @Test
    void compiledMethodExpressionVariablesAreBoundWhenTheExpressionIsCreated() {
        context.getVariableMapper().setVariable("action", factory.createValueExpression(
            action("first"), LambdaExpression.class));
        MethodExpression expression = factory.createMethodExpression(context, "${action}", String.class,
            new Class<?>[]{String.class});

        context.getVariableMapper().setVariable("action", factory.createValueExpression(
            action("second"), LambdaExpression.class));

        assertEquals("first:x", expression.invoke(context, new Object[]{"x"}));
    }

    private LambdaExpression action(String prefix) {
        return ELLambdas.create(context, List.of("value"),
            lambda -> prefix + ":" + lambda.getLambdaArgument("value"));
    }

    @Test
    void theStreamResolverIsProvided() {
        assertNotNull(factory.getStreamELResolver());
    }
}
