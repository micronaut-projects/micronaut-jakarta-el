package io.micronaut.el.test.parity;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.micronaut.el.test.parity.ParitySandboxExpressions$ELExpressions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Asserts the results {@code ELSandboxTest} asserts for the interpreted expressions, in a context holding the
 * same beans.
 */
class ParitySandboxExpressionsTest {

    private final CompiledELContext context = new CompiledELContext()
        .setBean("bean", "hello")
        .setBean("list", new ArrayList<>(List.of(1L, 2L, 3L)))
        .setBean("map", new LinkedHashMap<>(Map.of("k", "v")));

    @Test
    void theLanguageItselfIsUntouched() {
        assertEquals("HELLO", value(UPPER_CASE));
        assertEquals((Object) 5, value(LENGTH));
        assertEquals((Object) 7, value(INTEGER_VALUE_OF));
        assertEquals((Object) 2L, value(MAXIMUM));
        assertEquals(List.of(2L, 4L, 6L), value(DOUBLED));
        assertEquals((Object) 6L, value(SUM));
        assertEquals("v", value(MAP_ENTRY));
        assertEquals((Object) 1L, value(FIRST_ELEMENT));
        assertEquals((Object) 6L, value(ASSIGNMENT));
    }

    @Test
    void aCompiledExpressionIsNotSandboxed() {
        // the interpreted expression answers the same only under ELSandbox.UNRESTRICTED
        assertEquals("java.lang.String", value(CLASS_NAME));
    }

    private Object value(ValueExpression expression) {
        return expression.getValue(context);
    }
}
