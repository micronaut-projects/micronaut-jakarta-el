package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The operators inlined from the primitive types of their operands produce what the runtime operators produce.
 */
class InlinedOperatorExpressionsTest {

    private final ELContext context = new CompiledELContext()
        .setBean("item", new Inventory("A-1", 5, 10L, 9.99d, 1.5f, true, Suit.HEART));

    private final ELContext unnamed = new CompiledELContext()
        .setBean("item", new Inventory(null, 5, 10L, 9.99d, 1.5f, false, Suit.SPADE));

    @Test
    void arithmetic() {
        assertEquals(6L, value(InlinedOperatorExpressions$ELExpressions.ADD_LITERAL));
        assertEquals(15L, value(InlinedOperatorExpressions$ELExpressions.ADD_INTEGRAL));
        assertEquals(49.95d, value(InlinedOperatorExpressions$ELExpressions.MULTIPLY_MIXED));
        assertEquals(8.49d, value(InlinedOperatorExpressions$ELExpressions.SUBTRACT_FLOATING));
        assertEquals(2.5d, value(InlinedOperatorExpressions$ELExpressions.DIVIDE_INTEGRAL));
        assertEquals(1L, value(InlinedOperatorExpressions$ELExpressions.MODULO_INTEGRAL));
        assertEquals(9.99d % 2, value(InlinedOperatorExpressions$ELExpressions.MODULO_FLOATING));
    }

    @Test
    void negationKeepsTheTypeOfTheOperand() {
        assertEquals(-5, value(InlinedOperatorExpressions$ELExpressions.NEGATE_INT));
        assertEquals(-10L, value(InlinedOperatorExpressions$ELExpressions.NEGATE_LONG));
        assertEquals(-9.99d, value(InlinedOperatorExpressions$ELExpressions.NEGATE_DOUBLE));
        assertEquals(-1.5f, value(InlinedOperatorExpressions$ELExpressions.NEGATE_FLOAT));
    }

    @Test
    void comparisons() {
        assertEquals(true, value(InlinedOperatorExpressions$ELExpressions.GREATER_THAN));
        assertEquals(true, value(InlinedOperatorExpressions$ELExpressions.LESS_THAN_OR_EQUAL));
        assertEquals(true, value(InlinedOperatorExpressions$ELExpressions.EQUAL_INTEGRAL));
        assertEquals(false, value(InlinedOperatorExpressions$ELExpressions.NOT_EQUAL_INTEGRAL));
        assertEquals(true, value(InlinedOperatorExpressions$ELExpressions.GREATER_THAN_OR_EQUAL_FLOATING));
        assertEquals(true, value(InlinedOperatorExpressions$ELExpressions.LESS_THAN_MIXED));
        assertEquals(true, value(InlinedOperatorExpressions$ELExpressions.EQUAL_MIXED));
    }

    @Test
    void floatingPointComparisonsFollowTheRuntime() {
        // the runtime compares floating point values with Double.compare, which orders NaN above everything
        assertEquals(true, value(InlinedOperatorExpressions$ELExpressions.NAN_GREATER_THAN));
        assertEquals(false, value(InlinedOperatorExpressions$ELExpressions.NAN_LESS_THAN));
        assertEquals(true, value(InlinedOperatorExpressions$ELExpressions.NAN_EQUAL));
    }

    @Test
    void equalityOfStringsAndBooleans() {
        assertEquals(true, value(InlinedOperatorExpressions$ELExpressions.STRING_EQUAL));
        assertEquals(false, value(InlinedOperatorExpressions$ELExpressions.STRING_NOT_EQUAL));
        assertEquals(false, InlinedOperatorExpressions$ELExpressions.STRING_EQUAL.getValue(unnamed));
        assertEquals(true, InlinedOperatorExpressions$ELExpressions.STRING_NOT_EQUAL.getValue(unnamed));
        assertEquals(true, value(InlinedOperatorExpressions$ELExpressions.BOOLEAN_EQUAL));
        assertEquals(false, InlinedOperatorExpressions$ELExpressions.BOOLEAN_EQUAL.getValue(unnamed));
    }

    @Test
    void logical() {
        assertEquals(false, value(InlinedOperatorExpressions$ELExpressions.NOT));
        assertEquals(true, value(InlinedOperatorExpressions$ELExpressions.AND));
        assertEquals(false, InlinedOperatorExpressions$ELExpressions.AND.getValue(unnamed));
        assertEquals(true, InlinedOperatorExpressions$ELExpressions.OR.getValue(unnamed));
        assertEquals("in stock", value(InlinedOperatorExpressions$ELExpressions.TERNARY));
        assertEquals("sold out", InlinedOperatorExpressions$ELExpressions.TERNARY.getValue(unnamed));
    }

    @Test
    void stringConcatenationCoercesTheOperands() {
        assertEquals("A-1 x5", value(InlinedOperatorExpressions$ELExpressions.CONCAT));
        assertEquals(" x5", InlinedOperatorExpressions$ELExpressions.CONCAT.getValue(unnamed));
        assertEquals("SKU A-1 qty 5 suit HEART weight 1.5 ok true", value(InlinedOperatorExpressions$ELExpressions.COMPOSITE));
        assertEquals("SKU  qty 5 suit SPADE weight 1.5 ok false", InlinedOperatorExpressions$ELExpressions.COMPOSITE.getValue(unnamed));
    }

    private Object value(ValueExpression expression) {
        return expression.getValue(context);
    }
}
