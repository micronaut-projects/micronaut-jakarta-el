package io.micronaut.el.runtime;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import jakarta.el.MethodNotFoundException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Supplier;
import java.util.function.Predicate;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ELSupportTest {

    enum Suit {
        HEART,
        SPADE
    }

    static final class Varargs {
        public String join(String... values) {
            return String.join(",", java.util.Arrays.stream(values).map(String::valueOf).toList());
        }
    }

    static final class Overloads {
        public String target(Long first, Long second) {
            return "longs";
        }

        public String target(String first, String... rest) {
            return "strings";
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
    void aKnownFunctionalInterfaceIsImplementedWithoutAProxy() {
        CompiledELContext context = new CompiledELContext();
        LambdaExpression lambda = ELLambdas.create(context, List.of("value"),
            evaluated -> evaluated.getLambdaArgument("value"));

        Function<Object, Object> function = ELSupport.coerceToType(context, lambda, Function.class);
        Supplier<?> supplier = ELSupport.coerceToType(context, ELLambdas.create(context, List.of(),
            evaluated -> "supplied"), Supplier.class);

        assertEquals("mapped", function.apply("mapped"));
        assertEquals("supplied", supplier.get());
        // the interface is one this module implements, so nothing reflective stands in for it
        assertFalse(Proxy.isProxyClass(function.getClass()), function.getClass().getName());
        assertFalse(Proxy.isProxyClass(supplier.getClass()), supplier.getClass().getName());
    }

    @Test
    void theDeclaredParameterTypesAlsoSelectTheValueOfOverload() {
        CompiledELContext context = new CompiledELContext();

        // declared as taking a String, so the argument is parsed rather than read as a number
        assertEquals(12, context.getELResolver().invoke(context, new jakarta.el.ELClass(Integer.class),
            "valueOf", new Class<?>[]{String.class}, new Object[]{"12"}));
        // declared as taking an int, so a string argument is coerced to one
        assertEquals(12, context.getELResolver().invoke(context, new jakarta.el.ELClass(Integer.class),
            "valueOf", new Class<?>[]{int.class}, new Object[]{"12"}));
    }

    @Test
    void theDeclaredParameterTypesSelectTheOverloadRatherThanTheArgumentClasses() {
        CompiledELContext context = new CompiledELContext();

        // the values are Integers, but the expression declared long parameters: section 1.6 selects from what
        // was declared, so the long overload runs and the result is a Long
        Object max = context.getELResolver().invoke(context, new jakarta.el.ELClass(Math.class), "max",
            new Class<?>[]{long.class, long.class}, new Object[]{1, 2});

        assertEquals(2L, max);
    }

    @Test
    void aStaticMethodDeclaringNoParameterTypesIsDeclinedRatherThanFailing() {
        CompiledELContext context = new CompiledELContext();

        // a method expression can declare no parameter types at all: the resolver must decline and let the
        // chain continue, not index an empty array
        assertThrows(jakarta.el.MethodNotFoundException.class,
            () -> context.getELResolver().invoke(context, new jakarta.el.ELClass(Math.class), "max",
                new Class<?>[0], new Object[0]));
    }

    @Test
    void aDirectMathMethodCoercesTheArgumentsSelectedByTheirDeclaredTypes() {
        CompiledELContext context = new CompiledELContext();

        // the overload is selected from the parameter types a method expression declared, so the arguments
        // still have to be coerced to them at the invocation
        Object max = context.getELResolver().invoke(context, new jakarta.el.ELClass(Math.class), "max",
            new Class<?>[]{int.class, int.class}, new Object[]{"1", "2"});
        Object min = context.getELResolver().invoke(context, new jakarta.el.ELClass(Math.class), "min",
            new Class<?>[]{long.class, long.class}, new Object[]{"3", "4"});

        assertEquals(2, max);
        assertEquals(3L, min);
    }

    @Test
    void aComparatorReturningNullCoercesToTheDefaultOfItsPrimitiveReturnType() {
        CompiledELContext context = new CompiledELContext();
        Comparator<Object> comparator = ELSupport.coerceToType(context, ELLambdas.create(context,
            List.of("first", "second"), evaluated -> null), Comparator.class);

        // compare() returns a primitive, so the null coerces to zero instead of failing to unbox
        assertEquals(0, comparator.compare("a", "b"));
    }

    @Test
    void anArrayOfAnyComponentTypeIsCreatedWithoutReflection() {
        assertEquals(int[].class, ELArray.newInstance(int.class, 2).getClass());
        assertEquals(boolean[].class, ELArray.newInstance(boolean.class, 0).getClass());
        assertEquals(double[].class, ELArray.newInstance(double.class, 1).getClass());
        assertEquals(String[].class, ELArray.newInstance(String.class, 3).getClass());
        assertEquals(3, ((String[]) ELArray.newInstance(String.class, 3)).length);
        assertNull(((String[]) ELArray.newInstance(String.class, 1))[0]);
    }

    @Test
    void aPredicateAndAComparatorCoerceTheirResultToTheDeclaredReturnType() {
        CompiledELContext context = new CompiledELContext();

        Predicate<Object> predicate = ELSupport.coerceToType(context, ELLambdas.create(context, List.of("value"),
            evaluated -> "true"), Predicate.class);
        Comparator<Object> comparator = ELSupport.coerceToType(context, ELLambdas.create(context,
            List.of("first", "second"), evaluated -> "-1"), Comparator.class);

        assertTrue(predicate.test("ignored"));
        assertEquals(-1, comparator.compare("a", "b"));
        assertFalse(Proxy.isProxyClass(predicate.getClass()));
    }

    @Test
    void functionalInterfaceProxyKeepsTheContextItWasCreatedWith() {
        CompiledELContext firstContext = new CompiledELContext().setBean("value", "first");
        CompiledELContext secondContext = new CompiledELContext().setBean("value", "second");
        assertContextIsCaptured(firstContext, secondContext, ELLambdas.create(firstContext, List.of(),
            evaluated -> ((CompiledELContext) evaluated).getBean("value")));
        assertContextIsCaptured(firstContext, secondContext, ELLambdas.lambda0(firstContext,
            evaluated -> ((CompiledELContext) evaluated).getBean("value")));
    }

    @Test
    void onlyTheInvokePropertyInvokesALambdaMethodExpression() {
        CompiledELContext context = new CompiledELContext();
        LambdaExpression lambda = ELLambdas.create(context, List.of(), evaluated -> "called");

        assertThrows(MethodNotFoundException.class,
            () -> ELResolution.invokeWithParamTypes(context, lambda, "other", new Class<?>[0], null));
        assertEquals("called", ELResolution.invokeWithParamTypes(context, lambda, "invoke", new Class<?>[0], null));
    }

    private static void assertContextIsCaptured(CompiledELContext firstContext,
                                                CompiledELContext secondContext,
                                                LambdaExpression lambda) {
        Supplier<?> first = ELSupport.coerceToType(firstContext, lambda, Supplier.class);
        Supplier<?> second = ELSupport.coerceToType(secondContext, lambda, Supplier.class);

        assertEquals("first", first.get());
        assertEquals("second", second.get());
        assertEquals("first", first.get());
    }
}
