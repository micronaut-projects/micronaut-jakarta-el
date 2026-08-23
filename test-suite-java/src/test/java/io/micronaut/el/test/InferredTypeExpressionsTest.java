package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class InferredTypeExpressionsTest {

    private final ExpressionFactory factory = ExpressionFactory.newInstance();
    private final ELContext context = new CompiledELContext()
        .setBean("item", new Inventory("A-1", 5, 10L, 9.99d, 1.5f, true, Suit.HEART));

    @Test
    void theExpectedTypeIsTheStaticTypeOfTheExpression() {
        assertEquals(Double.class, InferredTypeExpressions$ELExpressions.PRICE.getExpectedType());
        assertEquals(Boolean.class, InferredTypeExpressions$ELExpressions.IN_STOCK.getExpectedType());
        assertEquals(String.class, InferredTypeExpressions$ELExpressions.SKU.getExpectedType());
        assertEquals(List.class, InferredTypeExpressions$ELExpressions.TAGS.getExpectedType());
        assertEquals(Suit.class, InferredTypeExpressions$ELExpressions.SUIT.getExpectedType());
    }

    @Test
    void theResultsHaveTheInferredTypes() {
        assertEquals(9.99d, InferredTypeExpressions$ELExpressions.PRICE.getValue(context));
        assertEquals(true, InferredTypeExpressions$ELExpressions.IN_STOCK.getValue(context));
        assertEquals("A-1", InferredTypeExpressions$ELExpressions.SKU.getValue(context));
        assertEquals(List.of("new", "sale", "b"), InferredTypeExpressions$ELExpressions.TAGS.getValue(context));
        assertEquals(Suit.HEART, InferredTypeExpressions$ELExpressions.SUIT.getValue(context));
    }

    @Test
    void aNullStringCoercesToTheEmptyStringOfTheInferredType() {
        ELContext unnamed = new CompiledELContext()
            .setBean("item", new Inventory(null, 5, 10L, 9.99d, 1.5f, true, Suit.HEART));
        assertEquals("", InferredTypeExpressions$ELExpressions.SKU.getValue(unnamed));
    }

    @Test
    void theFactoryServesTheInferredTypeItsPrimitiveAndObject() {
        assertSame(InferredTypeExpressions$ELExpressions.PRICE.getClass(),
            factory.createValueExpression(context, "${item.price}", Double.class).getClass());
        assertNotNull(factory.createValueExpression(context, "${item.price}", double.class));
        assertNotNull(factory.createValueExpression(context, "${item.price}", Object.class));
    }

    @Test
    void aMethodExpressionInfersItsReturnType() {
        assertEquals(2L, InferredTypeExpressions$ELExpressions.COUNT.invoke(context, null));
        assertNotNull(factory.createMethodExpression(context, "${item.count(t -> t.length() > 1)}", Long.class, new Class<?>[0]));
        assertNotNull(factory.createMethodExpression(context, "${item.count(t -> t.length() > 1)}", long.class, new Class<?>[0]));
        assertNotNull(factory.createMethodExpression(context, "${item.count(t -> t.length() > 1)}", Object.class, new Class<?>[0]));
    }
}
