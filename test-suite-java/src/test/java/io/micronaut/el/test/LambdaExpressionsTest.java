package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LambdaExpressionsTest {

    private final ELContext context = new CompiledELContext()
        .setBean("item", new Inventory("A-1", 5, 10L, 9.99d, 1.5f, true, Suit.HEART));

    @Test
    void streamOperationsTakeJavaLambdas() {
        assertEquals(List.of("new!", "sale!"), value(LambdaExpressions$ELExpressions.STREAM_PIPELINE));
        assertEquals(List.of("sale", "new", "b"), value(LambdaExpressions$ELExpressions.STREAM_SORTED));
        assertEquals(true, value(LambdaExpressions$ELExpressions.STREAM_ANY_MATCH));
        assertEquals("newsaleb", value(LambdaExpressions$ELExpressions.STREAM_REDUCE));
        assertEquals("fallback", value(LambdaExpressions$ELExpressions.OPTIONAL_SUPPLIER));
        assertNull(value(LambdaExpressions$ELExpressions.STREAM_FOR_EACH));
    }

    @Test
    void functionalInterfacesOfBeansTakeJavaLambdas() {
        assertEquals(2L, value(LambdaExpressions$ELExpressions.FUNCTIONAL_INTERFACE));
        assertEquals("A-1:5", value(LambdaExpressions$ELExpressions.FUNCTIONAL_INTERFACE_TYPED));
        assertEquals(3, value(LambdaExpressions$ELExpressions.OPTIONAL_MAP));
        // a custom functional interface with primitive parameters: the lambda is compiled to it directly
        assertEquals(9.99d * 5 + 1, value(LambdaExpressions$ELExpressions.CUSTOM_FUNCTIONAL_INTERFACE));
    }

    @Test
    void lambdaValuesAreCompiledLambdaExpressions() {
        assertEquals(10L, value(LambdaExpressions$ELExpressions.IMMEDIATE_INVOCATION));
        assertEquals(3L, value(LambdaExpressions$ELExpressions.LAMBDA_VARIABLE));
        assertEquals(3L, value(LambdaExpressions$ELExpressions.NESTED_LAMBDA));
        assertEquals(10L, value(LambdaExpressions$ELExpressions.FOUR_PARAMETERS));
        assertEquals(5, value(LambdaExpressions$ELExpressions.NO_PARAMETERS));
        assertEquals(List.of(6L, 4L), value(LambdaExpressions$ELExpressions.DYNAMIC_STREAM));
    }

    @Test
    void aMissingArgumentIsReported() {
        ELException e = assertThrows(ELException.class, () -> value(LambdaExpressions$ELExpressions.MISSING_ARGUMENT));
        assertEquals("Expected Argument y missing in Lambda Expression", e.getMessage());
    }

    private Object value(ValueExpression expression) {
        return expression.getValue(context);
    }
}
