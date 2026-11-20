package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.micronaut.el.test.CompilerRegressionExpressions$ELExpressions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The compile-time generated regressions of the compiler. Every expression here has an interpreted counterpart
 * with the same text and result in {@code io.micronaut.el.interpreter.CompilerRegressionInterpretedTest}.
 */
class CompilerRegressionExpressionsTest {

    private final ELContext context = new CompiledELContext().setBean("probe", new CompilerRegressionProbe());

    @BeforeEach
    void clearTheLog() {
        CompilerRegressionProbe.LOG.clear();
    }

    private Object value(ValueExpression expression) {
        return expression.getValue(context);
    }

    private static List<String> log() {
        return CompilerRegressionProbe.LOG;
    }

    @Test
    void theOperandsThatAreNotTakenAreNotEvaluated() {
        assertEquals("t", value(SHORT_TERNARY));
        assertEquals(List.of("t"), log());
        log().clear();
        assertEquals(false, value(SHORT_AND));
        assertEquals(true, value(SHORT_OR));
        assertEquals(List.of(), log());
        assertEquals("n", value(SHORT_EMPTY));
        assertEquals(List.of("n"), log());
        log().clear();
        assertEquals("EL", value(SHORT_NESTED));
        assertEquals(List.of(), log());
    }

    @Test
    void anArgumentIsEvaluatedOnce() {
        assertEquals("str:arg", value(ARGUMENT_ONCE));
        assertEquals(List.of("arg"), log());
    }

    @Test
    void theOperandsAreEvaluatedFromLeftToRight() {
        assertEquals("xy", value(ORDER_CONCAT));
        assertEquals(List.of("x", "y"), log());
        log().clear();
        assertEquals(List.of("x", "y"), value(ORDER_LIST));
        assertEquals(List.of("x", "y"), log());
        log().clear();
        assertEquals(false, value(ORDER_EQUAL));
        assertEquals(List.of("x", "y"), log());
    }

    @Test
    void aPrimitivePropertyOfANullBaseIsNull() {
        assertEquals(false, value(NULL_PRIMITIVE_EQUAL));
        assertEquals(false, value(NULL_PRIMITIVE_LESS));
        assertEquals(false, value(NULL_PRIMITIVE_LESS_OR_EQUAL));
        assertEquals(false, value(NULL_PRIMITIVE_BOOLEAN));
        assertEquals(0L, value(NULL_PRIMITIVE_IN_LAMBDA));
        assertEquals(0L, value(NULL_PRIMITIVE_NEGATED));
        // the arithmetic of null is the arithmetic of zero
        assertEquals(1L, value(NULL_PRIMITIVE_ADDED));
    }

    @Test
    void aSingleArgumentOfAVariableArityMethod() {
        assertEquals("EL#1", value(VARARGS_SINGLE));
        assertEquals("p+q#2", value(VARARGS_ARRAY));
        assertEquals("a#1", value(FUNCTION_VARARGS_SINGLE));
    }

    @Test
    void anIdentifierThatIsNotEvaluatedIsNotResolved() {
        assertEquals(true, value(UNDEFINED_NOT_RESOLVED));
    }

    @Test
    void aLambdaParameterHoldingADollar() {
        assertEquals(List.of("a", "b", "c"), value(DOLLAR_PARAMETER));
    }

    @Test
    void aStaticMethodInvokedThroughAnInstance() {
        assertEquals("static", value(STATIC_THROUGH_INSTANCE));
        assertEquals("5", value(STATIC_OVERLOAD_THROUGH_INSTANCE));
    }

    @Test
    void aVoidFunctionEvaluatesToNull() {
        assertNull(value(VOID_FUNCTION));
        assertEquals(List.of("nothing"), log());
    }

    @Test
    void argumentsTheRuntimeCoercionAccepts() {
        assertEquals("cmp:300", value(INT_TO_COMPARABLE));
        assertEquals("cmp:5", value(NUMBER_TO_COMPARABLE));
        assertEquals("ser:true", value(BOOLEAN_TO_SERIALIZABLE));
        assertEquals("cmp:cs", value(CHAR_SEQUENCE_TO_COMPARABLE));
        // compiles, and fails as the interpreter does: the value is a String, not a List
        assertThrows(ELException.class, () -> value(CHAR_SEQUENCE_TO_LIST));
        assertEquals("cmp:5", value(IMPORTED_STATIC_COERCION));
        assertEquals("resource:5", value(CONSTRUCTOR_COERCION));
    }

    @Test
    void aLambdaReturningNullToATypeVariablePassesNull() {
        assertEquals("got:null", value(SUPPLIER_NULL));
        assertEquals("got:null", value(FUNCTION_NULL));
    }

    @Test
    void whatAMemberThrowsIsAnELException() {
        ELException thrown = assertThrows(ELException.class, () -> value(MEMBER_THROWS));
        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertEquals("ok", value(CHECKED_PASSES));
        assertInstanceOf(IOException.class, assertThrows(ELException.class, () -> value(CHECKED_THROWS)).getCause());
        assertInstanceOf(IOException.class,
            assertThrows(ELException.class, () -> value(CHECKED_FUNCTION_THROWS)).getCause());
        assertInstanceOf(IOException.class,
            assertThrows(ELException.class, () -> value(CHECKED_CONSTRUCTOR_THROWS)).getCause());
    }

    @Test
    void thePropertyAndTheMethodOfAnOptionalAreTheOnesOfItsValue() {
        assertThrows(PropertyNotFoundException.class, () -> value(OPTIONAL_PROPERTY));
        assertEquals(3, value(OPTIONAL_METHOD));
        assertNull(value(EMPTY_OPTIONAL_METHOD));
    }

    @Test
    void theJavadocOfAGeneratedClassHoldsTheExpressionAsWritten() throws IOException {
        // a Java build gets readable sources, whose Javadoc quotes the expression: a '$' as it is, not doubled,
        // and the characters of the HTML markup as entities
        Path source = Path.of("build/generated/sources/annotationProcessor/java/test/io/micronaut/el/test",
            "CompilerRegressionExpressions$Expression1.java");
        Assumptions.assumeTrue(Files.exists(source), "the generated sources are not where the build writes them");
        String text = Files.readString(source);
        assertTrue(text.contains("<code>${false &amp;&amp; probe.hit('a').length() &gt; 0}</code>"), text);
        assertFalse(text.contains("$${"), text);
    }

    @Test
    void aNullArgumentSelectsThePrimitiveOverload() {
        assertEquals("int", value(NULL_ARGUMENT_OVERLOAD));
    }
}
