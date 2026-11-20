package io.micronaut.el.test.parity;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELException;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static io.micronaut.el.test.parity.ParityInterpreterExpressions$ELExpressions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts the results {@code ELInterpreterTest} and {@code InterpretingELExpressionParserTest} assert for the
 * interpreted expressions.
 */
class ParityInterpreterExpressionsTest {

    private final CompiledELContext context = new CompiledELContext()
        .setBean("bean", new ParityHolder())
        .setBean("extra", "value");

    @Test
    void anOverloadIsSelectedTheWayTheSpecificationSelectsIt() {
        assertEquals((Object) 1, value(INTEGER_VALUE_OF));
        assertEquals((Object) 1L, value(LONG_VALUE_OF));
        assertEquals((Object) 1.0d, value(DOUBLE_VALUE_OF));
        assertEquals("1", value(STRING_VALUE_OF));
        assertEquals((Object) 'A', value(CHARACTER_VALUE_OF));
        assertEquals((Object) false, value(BOOLEAN_VALUE_OF_NUMBER));
        assertEquals((Object) true, value(BOOLEAN_VALUE_OF_STRING));
    }

    @Test
    void aStreamHoldingANullElementDoesNotFail() {
        assertEquals("", value(NULL_REDUCE));
        assertEquals("", value(NULL_MAX));
        assertEquals("", value(NULL_MIN));
        assertEquals("", value(NULL_FIND_FIRST));
        assertEquals((Object) 3L, value(STREAM_MAX));
        assertEquals((Object) 1L, value(STREAM_MIN));
        assertEquals((Object) 3L, value(STREAM_FIND_FIRST));
    }

    @Test
    void aComparisonThatTheOperandsCannotAnswerIsAnExpressionLanguageError() {
        assertThrows(ELException.class, () -> value(INCOMPARABLE_MIN));
        assertThrows(ELException.class, () -> value(INCOMPARABLE_SORTED));
    }

    @Test
    void aSetOrMapConstructionKeepsTheOrderItWasWrittenIn() {
        assertEquals("[b, a]", String.valueOf(value(ORDERED_STRING_SET)));
        assertEquals("[3, 1, 2]", String.valueOf(value(ORDERED_NUMBER_SET)));
        assertEquals("{b=1, a=2}", String.valueOf(value(ORDERED_MAP)));
    }

    @Test
    void aSemicolonExpressionIsNotAnLvalue() {
        assertTrue(SEMICOLON_NOT_LVALUE.isReadOnly(context));
        assertNull(SEMICOLON_NOT_LVALUE.getType(context));
        assertNull(SEMICOLON_NOT_LVALUE.getValueReference(context));
        assertEquals("n", value(SEMICOLON_NOT_LVALUE));
    }

    @Test
    void theExpressionsOfTheParserCacheRegression() {
        assertEquals((Object) 0L, value(ZERO));
        assertEquals("value", value(EXTRA));
    }

    private Object value(ValueExpression expression) {
        return expression.getValue(context);
    }
}
