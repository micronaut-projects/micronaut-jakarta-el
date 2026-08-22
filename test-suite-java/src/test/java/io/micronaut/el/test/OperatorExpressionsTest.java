package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperatorExpressionsTest {

    private final ELContext context = new CompiledELContext();

    @Test
    void arithmetic() {
        assertEquals(3L, value(OperatorExpressions$ELExpressions.ADD));
        assertEquals(7L, value(OperatorExpressions$ELExpressions.PRECEDENCE));
        assertEquals(3.5d, value(OperatorExpressions$ELExpressions.DIVIDE));
        assertEquals(1L, value(OperatorExpressions$ELExpressions.MODULO));
        assertEquals(-3L, value(OperatorExpressions$ELExpressions.NEGATE));
        assertEquals(2.5d, value(OperatorExpressions$ELExpressions.FLOATING_POINT));
        assertEquals(9L, value(OperatorExpressions$ELExpressions.PARENTHESES));
    }

    @Test
    void stringConcatenation() {
        assertEquals("abc", value(OperatorExpressions$ELExpressions.CONCAT));
    }

    @Test
    void relationalAndLogical() {
        assertEquals(true, value(OperatorExpressions$ELExpressions.LESS_THAN));
        assertEquals(true, value(OperatorExpressions$ELExpressions.COERCED_EQUALITY));
        assertEquals(true, value(OperatorExpressions$ELExpressions.NULL_EQUALITY));
        assertEquals(false, value(OperatorExpressions$ELExpressions.AND));
        assertEquals(true, value(OperatorExpressions$ELExpressions.OR));
        assertEquals(false, value(OperatorExpressions$ELExpressions.NOT));
    }

    @Test
    void emptyOperator() {
        assertEquals(true, value(OperatorExpressions$ELExpressions.EMPTY_STRING));
        assertEquals(false, value(OperatorExpressions$ELExpressions.EMPTY_LIST));
    }

    @Test
    void conditionalAndSemicolon() {
        assertEquals("yes", value(OperatorExpressions$ELExpressions.TERNARY));
        assertEquals(3L, value(OperatorExpressions$ELExpressions.SEMICOLON));
    }

    @Test
    void collectionConstruction() {
        assertEquals(List.of(1L, "two", 3L), value(OperatorExpressions$ELExpressions.LIST));
        assertEquals(Set.of(1L, 2L, 3L), value(OperatorExpressions$ELExpressions.SET));
        assertEquals(Map.of("one", 1L, "two", 2L), value(OperatorExpressions$ELExpressions.MAP));
    }

    private Object value(ValueExpression expression) {
        return expression.getValue(context);
    }
}
