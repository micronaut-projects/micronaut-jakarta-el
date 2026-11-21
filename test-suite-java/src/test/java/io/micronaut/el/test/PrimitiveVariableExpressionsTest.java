package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static io.micronaut.el.test.PrimitiveVariableExpressions$ELExpressions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The compile-time generated expressions of a variable declared {@code int.class}. Every expression here has an
 * interpreted counterpart with the same text and result in
 * {@code io.micronaut.el.interpreter.PrimitiveVariableInterpretedTest}.
 */
class PrimitiveVariableExpressionsTest {

    private final ELContext context = new CompiledELContext()
        .setBean("bean", new PrimitiveVariableBean())
        .setBean("count", 2)
        .setBean("none", null);

    private Object value(ValueExpression expression) {
        return expression.getValue(context);
    }

    @Test
    void aPrimitiveValueSelectsTheWrapperOverload() {
        assertEquals("Integer:2", value(VARIABLE));
        assertEquals("Integer:2", value(PROPERTY));
        assertEquals("Long:2", value(ARITHMETIC));
    }

    @Test
    void aPrimitiveVariableIsStillCoercedToAPrimitiveParameter() {
        assertEquals("abab", value(PRIMITIVE_PARAMETER));
        assertEquals("", value(NULL_TO_PRIMITIVE));
        assertEquals(3L, value(PRIMITIVE_ARITHMETIC));
    }

    @Test
    void aNullPrimitiveVariableIsNull() {
        assertEquals(1L, value(NULL_ARITHMETIC));
        assertEquals(true, value(NULL_COMPARISON));
        assertEquals(true, value(NULL_EMPTY));
    }
}
