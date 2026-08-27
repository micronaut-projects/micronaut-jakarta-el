package io.micronaut.el.runtime;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ELSupportTest {

    interface UnannotatedFunction {
        String apply(String value);

        @Override
        boolean equals(Object object);
    }

    enum Suit {
        HEART,
        SPADE
    }

    static final class Varargs {
        public String join(String... values) {
            return String.join(",", java.util.Arrays.stream(values).map(String::valueOf).toList());
        }
    }

    @Test
    void coerceToString() {
        assertEquals("", ELSupport.coerceToString(null));
        assertEquals("a", ELSupport.coerceToString("a"));
        assertEquals("SPADE", ELSupport.coerceToString(Suit.SPADE));
        assertEquals("1", ELSupport.coerceToString(1));
    }

    @Test
    void coerceToNumber() {
        assertNull(ELSupport.coerceToNumber(null, Long.class));
        assertEquals(0L, ELSupport.coerceToNumber(null, long.class));
        assertEquals(0L, ELSupport.coerceToNumber("", Long.class));
        assertEquals(42L, ELSupport.coerceToNumber("42", Long.class));
        assertEquals(new BigDecimal("1.5"), ELSupport.coerceToNumber("1.5", BigDecimal.class));
        assertEquals(BigInteger.valueOf(3), ELSupport.coerceToNumber(3.9d, BigInteger.class));
        assertEquals(Short.valueOf((short) 'a'), ELSupport.coerceToNumber('a', Short.class));
        assertThrows(ELException.class, () -> ELSupport.coerceToNumber(Boolean.TRUE, Long.class));
        assertThrows(ELException.class, () -> ELSupport.coerceToNumber("x", Long.class));
    }

    @Test
    void coerceToBoolean() {
        assertNull(ELSupport.coerceToBoolean(null, false));
        assertEquals(Boolean.FALSE, ELSupport.coerceToBoolean(null, true));
        assertEquals(Boolean.FALSE, ELSupport.coerceToBoolean("", false));
        assertEquals(Boolean.TRUE, ELSupport.coerceToBoolean("true", false));
    }

    @Test
    void coerceToCharacter() {
        assertNull(ELSupport.coerceToCharacter(null, false));
        assertEquals(Character.valueOf((char) 0), ELSupport.coerceToCharacter(null, true));
        assertEquals(Character.valueOf('a'), ELSupport.coerceToCharacter("abc", false));
        assertEquals(Character.valueOf('A'), ELSupport.coerceToCharacter(65, false));
    }

    @Test
    void coerceToEnum() {
        assertNull(ELSupport.coerceToEnum(null, Suit.class));
        assertNull(ELSupport.coerceToEnum("", Suit.class));
        assertEquals(Suit.SPADE, ELSupport.coerceToEnum("SPADE", Suit.class));
        assertThrows(ELException.class, () -> ELSupport.coerceToEnum("KING", Suit.class));
    }

    @Test
    void coerceToArray() {
        Object coerced = ELSupport.coerceToArray(new String[]{"1", "2"}, Integer.class);
        assertEquals(Integer.valueOf(2), ((Integer[]) coerced)[1]);
    }

    @Test
    void emptyOperator() {
        assertTrue(ELSupport.isEmpty(null));
        assertTrue(ELSupport.isEmpty(""));
        assertTrue(ELSupport.isEmpty(new int[0]));
        assertTrue(ELSupport.isEmpty(List.of()));
        assertTrue(ELSupport.isEmpty(Map.of()));
        assertFalse(ELSupport.isEmpty(" "));
        assertFalse(ELSupport.isEmpty(List.of(1)));
    }

    @Test
    void equality() {
        assertTrue(ELSupport.equals(null, null));
        assertFalse(ELSupport.equals(null, 1));
        assertTrue(ELSupport.equals(1, 1L));
        assertTrue(ELSupport.equals("10", 10));
        assertTrue(ELSupport.equals(Suit.SPADE, "SPADE"));
        assertTrue(ELSupport.equals("true", Boolean.TRUE));
        assertTrue(ELSupport.equals(new BigDecimal("2"), 2));
        assertFalse(ELSupport.notEquals(1, 1));
    }

    @Test
    void comparison() {
        assertTrue(ELSupport.lessThan(1, 2));
        assertFalse(ELSupport.lessThan(null, 2));
        assertFalse(ELSupport.greaterThan(null, 2));
        assertTrue(ELSupport.lessThanOrEqual(2, 2));
        assertTrue(ELSupport.greaterThanOrEqual(2, 2));
        assertTrue(ELSupport.lessThan("a", "b"));
        assertTrue(ELSupport.greaterThan(new BigDecimal("2.5"), 2));
    }

    @Test
    void relationalOperatorsOnlyTreatFloatAndDoubleInstancesAsFloatingPoint() {
        // the section 1.9.2 leaves strings to the lexical rule, the floating point notation rule is arithmetic only
        assertFalse(ELSupport.equals("1.5", "1.50"));
        assertTrue(ELSupport.lessThan("1.5", "1.50"));
        assertTrue(ELSupport.equals(1.5d, "1.5"));
        assertTrue(ELSupport.equals(1L, "1"));
        assertThrows(ELException.class, () -> ELSupport.equals(1L, "1.0"));
        // while the arithmetic rule still applies to strings
        assertEquals(2.5d, ELArithmetic.add("1.5", 1L));
    }

    @Test
    void lambdaCoercesToAnUnannotatedFunctionalInterface() {
        CompiledELContext context = new CompiledELContext();
        LambdaExpression lambda = ELLambdas.create(context, List.of("value"),
            evaluated -> evaluated.getLambdaArgument("value"));

        UnannotatedFunction function = ELSupport.coerceToType(context, lambda, UnannotatedFunction.class);

        assertEquals("lambda", function.apply("lambda"));
        assertEquals(function, function);
        assertFalse(function.equals(new Object()));
    }

    @Test
    void aSubtypeArrayIsPassedDirectlyToAVariableArityMethod() throws NoSuchMethodException {
        String[] values = {"a", "b"};
        Method join = ELMethods.findMethod(Varargs.class, "join", null, new Object[]{values});

        assertEquals("a,b", ELMethods.invoke(new CompiledELContext(), join, new Varargs(),
            new Object[]{values}));
    }
}
