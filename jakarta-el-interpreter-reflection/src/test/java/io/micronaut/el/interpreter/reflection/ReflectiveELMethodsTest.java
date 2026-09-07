package io.micronaut.el.interpreter.reflection;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.runtime.ELMethods;
import jakarta.el.ELContext;
import jakarta.el.MethodNotFoundException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reflective method selection of the section 1.6 of the specification, which the standard resolvers reach
 * for a type without a bean introspection.
 */
class ReflectiveELMethodsTest {

    private final ELContext context = new CompiledELContext();
    private final Bean bean = new Bean();

    private Object invoke(String name, Object... arguments) {
        Method method = ReflectiveELMethods.findMethod(Bean.class, name, null, arguments);
        return ReflectiveELMethods.invoke(context, method, bean, arguments);
    }

    @Test
    void theOverloadWhoseParametersTakeTheArgumentsWins() {
        assertEquals("long", invoke("pick", 1L));
        assertEquals("string", invoke("pick", "one"));
        // an argument neither overload takes as it is, coerced to the one that accepts it
        assertEquals("long", invoke("pick", 1));
    }

    @Test
    void aNullArgumentReachesThePrimitiveOverloadAsTheOtherImplementationsSelectIt() {
        // Java would rule the primitive parameter out and choose nullable(Object). Expressly and Jasper both
        // choose nullable(int), coercing the null to its default, and the differential fuzz test holds all
        // three implementations to that answer
        assertEquals("int", invoke("nullable", new Object[]{null}));
    }

    @Test
    void theMoreSpecificParameterWins() {
        assertEquals("number", invoke("widen", 1L));
        assertEquals("object", invoke("widen", List.of()));
    }

    @Test
    void aNumericParameterWinsForANumericArgument() {
        assertEquals("number", invoke("numeric", 1L));
    }

    @Test
    void theDeclarationWinsOverTheBridgeGeneratedForIt() {
        assertEquals("string", invoke("map", "value"));
    }

    @Test
    void aNullArgumentIsAssignableToAReferenceAndCoercibleToAPrimitive() {
        assertEquals("described", invoke("describe", new Object[]{null}));
        assertEquals(0, invoke("twice", new Object[]{null}));
        // a null argument fits both overloads equally, which the specification calls ambiguous
        assertThrows(MethodNotFoundException.class, () -> invoke("pick", new Object[]{null}));
    }

    @Test
    void variableArityPacksTheTrailingArgumentsAndTakesAnArrayDirectly() {
        assertEquals("2", invoke("join", "a", "b"));
        assertEquals("0", invoke("join"));
        assertEquals("2", invoke("join", (Object) new Object[]{"a", "b"}));
        // an array whose type is not the declared one is one of the packed arguments, not the array itself
        assertEquals("1", invoke("join", (Object) new String[]{"a", "b"}));
    }

    @Test
    void anOverloadTheArgumentsDoNotFitIsNotSelected() {
        assertEquals("fixed", invoke("mixed", "only"));
        assertEquals("varargs", invoke("mixed", "one", "two"));
    }

    @Test
    void aMethodTheTypeDoesNotDeclareIsReported() {
        MethodNotFoundException e = assertThrows(MethodNotFoundException.class,
            () -> ReflectiveELMethods.findMethod(Bean.class, "absent", null, new Object[0]));
        assertTrue(e.getMessage().contains("absent"), e.getMessage());

        assertNull(ReflectiveELMethods.findMethodOrNull(Bean.class, "absent", null, new Object[0], false));
        assertThrows(MethodNotFoundException.class,
            () -> ReflectiveELMethods.findStaticMethod(Bean.class, "absent", new Class<?>[0], null));
    }

    @Test
    void anArityNoOverloadAcceptsIsReported() {
        assertThrows(MethodNotFoundException.class, () -> invoke("twice", 1, 2, 3));
    }

    @Test
    void equallySpecificOverloadsAreAmbiguous() {
        assertThrows(MethodNotFoundException.class, () -> invoke("ambiguous", 1L));
    }

    @Test
    void theParameterTypesOfAMethodExpressionSelectTheOverloadOnTheirOwn() {
        Method declared = ReflectiveELMethods.findMethod(Bean.class, "pick", new Class<?>[]{String.class}, null);
        assertEquals(String.class, declared.getParameterTypes()[0]);
        assertTrue(ELMethods.sameTypes(new Class<?>[]{String.class}, new Class<?>[]{String.class}));
        assertFalse(ELMethods.sameTypes(new Class<?>[]{String.class}, new Class<?>[]{Long.class}));
        assertFalse(ELMethods.sameTypes(new Class<?>[0], new Class<?>[]{Long.class}));
    }

    @Test
    void invokingWithTheWrongNumberOfArgumentsIsRejected() {
        Method twice = ReflectiveELMethods.findMethod(Bean.class, "twice", null, new Object[]{1});
        assertThrows(IllegalArgumentException.class,
            () -> ReflectiveELMethods.invoke(context, twice, bean, new Object[]{1, 2}));

        Method mixed = ReflectiveELMethods.findMethod(Bean.class, "mixed", null, new Object[]{"one", "two"});
        assertThrows(IllegalArgumentException.class,
            () -> ReflectiveELMethods.invoke(context, mixed, bean, null));
    }

    /**
     * A bean without an introspection, so that the resolution of its methods is the reflective one.
     */
    interface Mapper<T> {
        String map(T value);
    }

    public static class Bean implements Mapper<String> {

        public String pick(Long value) {
            return "long";
        }

        public String pick(String value) {
            return "string";
        }

        public String nullable(int value) {
            return "int";
        }

        public String nullable(Object value) {
            return "object";
        }

        public String widen(Number value) {
            return "number";
        }

        public String widen(Object value) {
            return "object";
        }

        public String numeric(Number value) {
            return "number";
        }

        public String numeric(CharSequence value) {
            return "charSequence";
        }

        public String ambiguous(Comparable<?> value) {
            return "comparable";
        }

        public String ambiguous(java.io.Serializable value) {
            return "serializable";
        }

        public String describe(String value) {
            return "described";
        }

        public int twice(int value) {
            return value * 2;
        }

        public String join(Object... parts) {
            return String.valueOf(parts.length);
        }

        public String mixed(String only) {
            return "fixed";
        }

        public String mixed(String first, String... rest) {
            return "varargs";
        }

        @Override
        public String map(String value) {
            return "string";
        }
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
    void aSubtypeArrayIsPassedDirectlyToAVariableArityMethod() throws NoSuchMethodException {
        String[] values = {"a", "b"};
        Method join = ReflectiveELMethods.findMethod(Varargs.class, "join", null, new Object[]{values});

        assertEquals("a,b", ReflectiveELMethods.invoke(new CompiledELContext(), join, new Varargs(),
            new Object[]{values}));
    }

    @Test
    void overloadSelectionChecksWhetherTheActualValueCanBeCoerced() {
        Method numeric = ReflectiveELMethods.findMethod(Overloads.class, "target", null, new Object[]{"1", "1"});
        Method text = ReflectiveELMethods.findMethod(Overloads.class, "target", null, new Object[]{"aaa", "bbb"});

        assertEquals(Long.class, numeric.getParameterTypes()[0]);
        assertEquals(String[].class, text.getParameterTypes()[1]);
    }

}
