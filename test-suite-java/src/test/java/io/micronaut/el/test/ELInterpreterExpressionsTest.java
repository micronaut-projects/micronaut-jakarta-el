package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.runtime.ELLambdas;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.EvaluationListener;
import jakarta.el.MethodExpression;
import jakarta.el.MethodNotFoundException;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ELInterpreterExpressionsTest {

    private final ELContext context = new CompiledELContext()
        .setBean("greeting", "hello")
        .setBean("xs", List.of(1, 2, 3))
        .setBean("bean", new Varargs())
        .setBean("sequences", new CharSequence[]{"a", "b"})
        .setBean("varargs", new Varargs())
        .setBean("strings", new String[]{"a", "b"})
        .setBean("numbers", new int[]{1, 2, 3})
        .setBean("functions", new Formatting())
        .setBean("f", new Formatting())
        .setBean("formatting", new Formatting())
        .setBean("integer", 1)
        .setBean("number", 1)
        .setBean("item", new Inventory("A-1", 5, 10L, 9.99d, 1.5f, true, Suit.HEART))
        .setBean("book", new Book("EL", "history", 20d))
        .setBean("decimal", new BigDecimal("9007199254740993"))
        .setBean("large", 9007199254740993L)
        .setBean("counter", new EvaluationCounter())
        .setBean("twice", 42L)
        .setBean("target", ELInterpreterExpressions$ELExpressions.LIST_SIZE_METHOD);

    {
        ((CompiledELContext) context).setBean("shadow", ELLambdas.create(context, List.of("value"),
            lambdaContext -> "variable:" + lambdaContext.getLambdaArgument("value")));
    }

    @Test
    void literalsAndOperators() {
        assertEquals(3L, value(ELInterpreterExpressions$ELExpressions.ADDITION));
        assertEquals("3", value(ELInterpreterExpressions$ELExpressions.ADDITION_AS_STRING));
        assertEquals(2.5d, value(ELInterpreterExpressions$ELExpressions.DIVISION));
        assertEquals(1L, value(ELInterpreterExpressions$ELExpressions.MODULO));
        assertEquals(-3L, value(ELInterpreterExpressions$ELExpressions.NEGATION));
        assertEquals("ab", value(ELInterpreterExpressions$ELExpressions.CONCATENATION));
        assertEquals(true, value(ELInterpreterExpressions$ELExpressions.LESS_THAN));
        assertEquals(true, value(ELInterpreterExpressions$ELExpressions.EQUALITY));
        assertEquals(false, value(ELInterpreterExpressions$ELExpressions.NULL_EQUALITY));
        assertEquals(false, value(ELInterpreterExpressions$ELExpressions.NULL_RELATIONAL_SHORT_CIRCUIT));
        assertEquals(false, value(ELInterpreterExpressions$ELExpressions.NULL_GREATER_THAN_SHORT_CIRCUIT));
        assertEquals(true, value(ELInterpreterExpressions$ELExpressions.LOGICAL));
        assertEquals(true, value(ELInterpreterExpressions$ELExpressions.EMPTY_NULL));
        assertEquals(true, value(ELInterpreterExpressions$ELExpressions.EMPTY_LIST));
        assertEquals("yes", value(ELInterpreterExpressions$ELExpressions.CONDITIONAL));
        assertEquals(2L, value(ELInterpreterExpressions$ELExpressions.SEMICOLON));
    }

    @Test
    void collectionsAndLambdas() {
        assertEquals(List.of(1L, 2L, 3L), value(ELInterpreterExpressions$ELExpressions.LIST));
        assertEquals(Map.of("one", 1L), value(ELInterpreterExpressions$ELExpressions.MAP));
        assertEquals(Set.of(1L, 2L, 3L), value(ELInterpreterExpressions$ELExpressions.SET));
        assertEquals(7L, value(ELInterpreterExpressions$ELExpressions.IMMEDIATE_LAMBDA));
        assertEquals(7L, value(ELInterpreterExpressions$ELExpressions.ASSIGNED_LAMBDA));
        assertEquals(120L, value(ELInterpreterExpressions$ELExpressions.FACTORIAL));
        assertEquals(3L, value(ELInterpreterExpressions$ELExpressions.NESTED_LAMBDA));
    }

    @Test
    void lambdaValuesResolveFreeVariablesWhenInvoked() {
        jakarta.el.LambdaExpression title = (jakarta.el.LambdaExpression) value(
            ELInterpreterExpressions$ELExpressions.DEFERRED_BOOK_TITLE);
        ((CompiledELContext) context).setBean("book", new Book("Updated", "history", 20d));

        assertEquals("Updated", title.invoke("unused"));
    }

    @Test
    void streamOperations() {
        assertEquals(List.of(2L, 4L), value(ELInterpreterExpressions$ELExpressions.FILTERED_STREAM));
        assertEquals(10L, value(ELInterpreterExpressions$ELExpressions.STREAM_SUM));
        assertEquals(4L, value(ELInterpreterExpressions$ELExpressions.STREAM_COUNT));
        assertEquals(List.of(1L, 2L, 3L), value(ELInterpreterExpressions$ELExpressions.SORTED_STREAM));
        assertEquals(3L, value(ELInterpreterExpressions$ELExpressions.STREAM_MAXIMUM));
        assertThrows(MethodNotFoundException.class,
            () -> value(ELInterpreterExpressions$ELExpressions.STREAM_COUNT_WRONG_ARITY));
        assertThrows(MethodNotFoundException.class,
            () -> value(ELInterpreterExpressions$ELExpressions.STREAM_FILTER_WRONG_ARITY));
        assertThrows(MethodNotFoundException.class,
            () -> value(ELInterpreterExpressions$ELExpressions.OPTIONAL_GET_WRONG_ARITY));
        assertThrows(ELException.class,
            () -> value(ELInterpreterExpressions$ELExpressions.NEGATIVE_STREAM_LIMIT));
        assertThrows(ELException.class,
            () -> value(ELInterpreterExpressions$ELExpressions.REVERSED_SUBSTREAM));
        assertNull(value(ELInterpreterExpressions$ELExpressions.OPTIONAL_IF_PRESENT));
        assertNull(value(ELInterpreterExpressions$ELExpressions.OPTIONAL_NULL_FALLBACK));
        assertEquals(List.of(), value(ELInterpreterExpressions$ELExpressions.NULL_STREAM_LIMIT));
        assertEquals(List.of(1L), value(ELInterpreterExpressions$ELExpressions.NULL_STREAM_START));
        assertEquals(1L, value(ELInterpreterExpressions$ELExpressions.NULL_REDUCTION_SEED));
    }

    @Test
    void nullBasesShortCircuitPropertiesAndMethodArguments() {
        EvaluationCounter counter = new EvaluationCounter();
        ELContext nullContext = new CompiledELContext()
            .setBean("counter", counter)
            .setBean("book", null)
            .setBean("xs", null);

        assertNull(ELInterpreterExpressions$ELExpressions.NULL_BASE_PROPERTY_SHORT_CIRCUIT.getValue(nullContext));
        assertNull(ELInterpreterExpressions$ELExpressions.NULL_BASE_METHOD_SHORT_CIRCUIT.getValue(nullContext));
        assertNull(ELInterpreterExpressions$ELExpressions.NULL_TYPED_PROPERTY.getValue(nullContext));
        assertNull(ELInterpreterExpressions$ELExpressions.NULL_TYPED_METHOD.getValue(nullContext));
        assertNull(ELInterpreterExpressions$ELExpressions.NULL_TYPED_COLLECTION.getValue(nullContext));
        assertNull(ELInterpreterExpressions$ELExpressions.NULL_TYPED_STREAM.getValue(nullContext));
        assertThrows(jakarta.el.PropertyNotFoundException.class,
            () -> ELInterpreterExpressions$ELExpressions.NULL_BASE_ASSIGNMENT.getValue(nullContext));
        assertEquals(0, counter.getCalls());
    }

    @Test
    void beansStaticReferencesAndConstructors() {
        assertEquals("hello", value(ELInterpreterExpressions$ELExpressions.GREETING));
        assertEquals(5, value(ELInterpreterExpressions$ELExpressions.GREETING_LENGTH));
        assertEquals("HELLO", value(ELInterpreterExpressions$ELExpressions.UPPERCASE_GREETING));
        assertEquals("HEL", value(ELInterpreterExpressions$ELExpressions.SHORT_UPPERCASE_GREETING));
        assertEquals(2, value(ELInterpreterExpressions$ELExpressions.STRINGS_LENGTH));
        assertEquals(2, value(ELInterpreterExpressions$ELExpressions.PRIMITIVE_ARRAY_ELEMENT));
        assertEquals(6L, value(ELInterpreterExpressions$ELExpressions.PRIMITIVE_ARRAY_STREAM_SUM));
        assertEquals(4, value(ELInterpreterExpressions$ELExpressions.PRIMITIVE_ARRAY_ASSIGNMENT));
        assertEquals(Boolean.TRUE, value(ELInterpreterExpressions$ELExpressions.BOOLEAN_CONSTANT));
        assertEquals(3, value(ELInterpreterExpressions$ELExpressions.INTEGER_VALUE_OF));
        assertEquals("x", value(ELInterpreterExpressions$ELExpressions.STRING_CONSTRUCTOR));
        assertEquals("a,b", value(ELInterpreterExpressions$ELExpressions.VARARGS_CONSTRUCTOR));
    }

    @Test
    void functionsVarargsArraysAndFunctionalInterfaces() {
        assertEquals("a,b", value(ELInterpreterExpressions$ELExpressions.FUNCTION_JOIN));
        assertEquals("a,b", value(ELInterpreterExpressions$ELExpressions.FUNCTION_ARRAY_JOIN));
        assertEquals("a,b", value(ELInterpreterExpressions$ELExpressions.FUNCTION_SUBTYPE_ARRAY_JOIN));
        assertEquals("a,b", value(ELInterpreterExpressions$ELExpressions.UNQUALIFIED_FUNCTION_JOIN));
        assertEquals("a,b", value(ELInterpreterExpressions$ELExpressions.NAMED_VARARGS_CONSTRUCTOR));
        assertEquals("1:int[]", value(ELInterpreterExpressions$ELExpressions.PRIMITIVE_VARARGS_ARRAY));
        assertEquals("a,b", value(ELInterpreterExpressions$ELExpressions.REFERENCE_VARARGS_ARRAY));
        assertEquals("a", value(ELInterpreterExpressions$ELExpressions.ARRAY_ELEMENT));
        assertEquals("default:EL", value(ELInterpreterExpressions$ELExpressions.FUNCTIONAL_INTERFACE));
        assertEquals("default:EL", value(ELInterpreterExpressions$ELExpressions.ALIASED_FUNCTIONAL_INTERFACE));
        assertEquals("1:int[]", value(ELInterpreterExpressions$ELExpressions.FORMATTING_PRIMITIVE_ARRAY));
        assertEquals("1:java.lang.String[]", value(ELInterpreterExpressions$ELExpressions.FORMATTING_REFERENCE_ARRAY));
        assertEquals(7, value(ELInterpreterExpressions$ELExpressions.STATIC_INTEGER_MAXIMUM));
        assertEquals(25d, value(ELInterpreterExpressions$ELExpressions.STATIC_DOUBLE_MAXIMUM));
        assertEquals(true, value(ELInterpreterExpressions$ELExpressions.LARGE_INTEGRAL_BIG_DECIMAL_EQUALITY));
        assertEquals(6L, value(ELInterpreterExpressions$ELExpressions.MAPPED_FUNCTION_FALLBACK));
        assertThrows(MethodNotFoundException.class,
            () -> value(ELInterpreterExpressions$ELExpressions.MISSING_FUNCTION));
        assertEquals("assignable", value(ELInterpreterExpressions$ELExpressions.ASSIGNABLE_OVER_COERCIBLE));
        assertEquals("number", value(ELInterpreterExpressions$ELExpressions.MOST_SPECIFIC_OVERLOAD));
        assertEquals("wrapper", value(ELInterpreterExpressions$ELExpressions.BOXED_OVERLOAD));
        assertEquals("integer", value(ELInterpreterExpressions$ELExpressions.RUNTIME_SUBTYPE_OVERLOAD));
        assertEquals(false, value(ELInterpreterExpressions$ELExpressions.NESTED_RELATIONAL_SHORT_CIRCUIT));
        assertEquals(0, ((EvaluationCounter) ((CompiledELContext) context).getBean("counter")).getCalls());
        assertEquals("variable:x", value(ELInterpreterExpressions$ELExpressions.LAMBDA_VARIABLE_SHADOWS_FUNCTION));
        assertThrows(MethodNotFoundException.class,
            () -> value(ELInterpreterExpressions$ELExpressions.EMPTY_VARARGS_AMBIGUITY));
        assertThrows(MethodNotFoundException.class,
            () -> value(ELInterpreterExpressions$ELExpressions.NUMERIC_AMBIGUITY));
        assertThrows(MethodNotFoundException.class,
            () -> value(ELInterpreterExpressions$ELExpressions.NON_FUNCTIONAL_INTERFACE));
        assertThrows(MethodNotFoundException.class,
            () -> value(ELInterpreterExpressions$ELExpressions.SEALED_INTERFACE));
    }

    @Test
    void methodExpressions() throws Exception {
        assertEquals(3, ELInterpreterExpressions$ELExpressions.LIST_SIZE_METHOD.invoke(context, null));
        assertEquals(42, ELInterpreterExpressions$ELExpressions.INTEGER_VALUE_OF_METHOD.invoke(context,
            new Object[]{"42"}));
        assertEquals("a,b", ELInterpreterExpressions$ELExpressions.VARARGS_METHOD.invoke(context,
            new Object[]{"a", "b"}));
        assertEquals("1:java.lang.String[]", ELInterpreterExpressions$ELExpressions.OBJECT_VARARGS_METHOD.invoke(context,
            new Object[]{new String[]{"a", "b"}}));
        assertEquals("number", ELInterpreterExpressions$ELExpressions.SPECIFIC_METHOD.invoke(context,
            new Object[]{1L}));
        assertEquals("String", ELInterpreterExpressions$ELExpressions.COMPATIBLE_METHOD.invoke(context,
            new Object[]{"value"}));
        assertEquals("a,b", ELInterpreterExpressions$ELExpressions.EXPANDED_VARARGS_METHOD.invoke(context,
            new Object[]{"a", "b"}));

        MethodExpression identifier = ELInterpreterExpressions$ELExpressions.IDENTIFIER_METHOD;
        assertEquals(3, identifier.invoke(context, null));
        assertEquals("size", identifier.getMethodInfo(context).getName());
        assertEquals("size", identifier.getMethodReference(context).getMethodInfo().getName());

        MethodExpression provided = roundTrip(ELInterpreterExpressions$ELExpressions.PROVIDED_VARARGS_METHOD);
        assertTrue(provided.isParametersProvided());
        assertEquals("a,b", provided.invoke(context, null));
    }

    @Test
    void listenersObserveMethodReferencesAndCompletedCoercions() {
        List<String> events = new ArrayList<>();
        CompiledELContext listeningContext = new CompiledELContext(new FailingIntegerConversionResolver(events))
            .setBean("varargs", new Varargs());
        listeningContext.addEvaluationListener(new RecordingListener(events));

        ELInterpreterExpressions$ELExpressions.COERCION_LISTENER_METHOD.getMethodReference(listeningContext);
        assertEquals(List.of("before:#{varargs.numberText}", "after:#{varargs.numberText}"), events);

        events.clear();
        assertThrows(ELException.class,
            () -> ELInterpreterExpressions$ELExpressions.COERCION_LISTENER_VALUE.getValue(listeningContext));
        assertEquals(List.of("before:${'1'}", "coerce"), events);

        events.clear();
        assertThrows(ELException.class,
            () -> ELInterpreterExpressions$ELExpressions.COERCION_LISTENER_METHOD.invoke(listeningContext, null));
        assertEquals(List.of("before:#{varargs.numberText}", "coerce"), events);
    }

    @Test
    void nullableExpectedReturnTypeReturnsTheUncoercedCompiledResult() {
        CompiledExpressionFactory factory = new CompiledExpressionFactory(
            List.of(new ELInterpreterExpressions$ELExpressions()));

        assertEquals(1, ELInterpreterExpressions$ELExpressions.COERCION_LISTENER_METHOD.invoke(context, null));
        assertEquals("1", factory.createMethodExpression(context, "#{varargs.numberText}", null,
            new Class<?>[0]).invoke(context, null));
    }

    @Test
    void generatedFunctionExpressionsSurviveSerialization() throws Exception {
        assertEquals("a,b", roundTrip(ELInterpreterExpressions$ELExpressions.FUNCTION_JOIN).getValue(context));
    }

    @Test
    void expressionEqualityUsesTheParsedAndBoundRepresentation() {
        ValueExpression addition = ELInterpreterExpressions$ELExpressions.ADDITION;
        ValueExpression coercedAddition = ELInterpreterExpressions$ELExpressions.ADDITION_AS_STRING;
        assertEquals(addition, coercedAddition);
        assertEquals(addition.hashCode(), coercedAddition.hashCode());

        ValueExpression joined = ELInterpreterExpressions$ELExpressions.FUNCTION_JOIN;
        ValueExpression aliased = ELInterpreterExpressions$ELExpressions.ALIASED_FUNCTION_JOIN;
        assertEquals(joined, aliased);
        assertEquals(joined.hashCode(), aliased.hashCode());
        org.junit.jupiter.api.Assertions.assertNotEquals(joined,
            ELInterpreterExpressions$ELExpressions.DIFFERENT_FUNCTION_JOIN);

        assertEquals(ELInterpreterExpressions$ELExpressions.LIST_SIZE_METHOD,
            ELInterpreterExpressions$ELExpressions.LIST_SIZE_STRING_METHOD);
        assertEquals(ELInterpreterExpressions$ELExpressions.LIST_SIZE_METHOD.hashCode(),
            ELInterpreterExpressions$ELExpressions.LIST_SIZE_STRING_METHOD.hashCode());

    }

    private Object value(ValueExpression expression) {
        return expression.getValue(context);
    }

    private static MethodExpression roundTrip(MethodExpression expression) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(expression);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (MethodExpression) input.readObject();
        }
    }

    private static ValueExpression roundTrip(ValueExpression expression) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(expression);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (ValueExpression) input.readObject();
        }
    }

    private static final class RecordingListener extends EvaluationListener {
        private final List<String> events;

        private RecordingListener(List<String> events) {
            this.events = events;
        }

        @Override
        public void beforeEvaluation(ELContext context, String expression) {
            events.add("before:" + expression);
        }

        @Override
        public void afterEvaluation(ELContext context, String expression) {
            events.add("after:" + expression);
        }
    }

    private static final class FailingIntegerConversionResolver extends ELResolver {
        private final List<String> events;

        private FailingIntegerConversionResolver(List<String> events) {
            this.events = events;
        }

        @Override
        public <T> T convertToType(ELContext context, Object value, Class<T> type) {
            if (type == Integer.class) {
                context.setPropertyResolved(true);
                events.add("coerce");
                throw new ELException("conversion failed");
            }
            return null;
        }

        @Override
        public Object getValue(ELContext context, Object base, Object property) {
            return null;
        }

        @Override
        public Class<?> getType(ELContext context, Object base, Object property) {
            return null;
        }

        @Override
        public void setValue(ELContext context, Object base, Object property, Object value) {
        }

        @Override
        public boolean isReadOnly(ELContext context, Object base, Object property) {
            return true;
        }

        @Override
        public Class<?> getCommonPropertyType(ELContext context, Object base) {
            return null;
        }
    }
}
