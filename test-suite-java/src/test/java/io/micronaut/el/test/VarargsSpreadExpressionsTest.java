package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static io.micronaut.el.test.VarargsSpreadExpressions$ELExpressions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The compile-time generated expressions that spread an array into a variable arity parameter. Every expression
 * here has an interpreted counterpart with the same text and result in
 * {@code io.micronaut.el.interpreter.VarargsSpreadInterpretedTest}.
 */
class VarargsSpreadExpressionsTest {

    private final ELContext context = new CompiledELContext()
        .setBean("bean", new VarargsSpreadBean())
        .setBean("strings", new String[]{"a", "b"})
        .setBean("objects", new Object[]{"a", "b"})
        .setBean("numbers", new int[]{1, 2});

    private Object value(ValueExpression expression) {
        return expression.getValue(context);
    }

    @Test
    void aStaticMethodOfTheJdkSpreadsTheArray() {
        assertEquals("a-b", value(STATIC_FORMAT));
    }

    @Test
    void anInstanceMethodSpreadsTheArray() {
        assertEquals("2:java.lang.String", value(INSTANCE_SUBTYPE_ARRAY));
        assertEquals("a-b", value(INSTANCE_JOIN));
    }

    @Test
    void aStaticMethodSpreadsTheArray() {
        assertEquals("2:java.lang.String", value(STATIC_THROUGH_CLASS));
        assertEquals("2:java.lang.String", value(STATIC_THROUGH_INSTANCE));
    }

    @Test
    void aFunctionSpreadsTheArray() {
        assertEquals("2:java.lang.String", value(FUNCTION));
        assertEquals("2:java.lang.String", value(STATIC_IMPORT));
    }

    @Test
    void aConstructorSpreadsTheArray() {
        assertEquals("2:java.lang.String", value(CONSTRUCTOR));
    }

    @Test
    void anArrayOfTheParameterTypeIsSpreadAndAPrimitiveArrayIsPacked() {
        assertEquals("2:java.lang.String", value(SAME_TYPE_ARRAY));
        assertEquals("1:int[]", value(PRIMITIVE_ARRAY));
    }
}
