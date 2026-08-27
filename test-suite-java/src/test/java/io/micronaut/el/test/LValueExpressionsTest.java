package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.ValueReference;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LValueExpressionsTest {

    private final Book book = new Book("EL", "history", 20d);
    private final Author author = new Author("Denis", List.of(book));
    private final CompiledELContext context = new CompiledELContext()
        .setBean("book", book)
        .setBean("author", author)
        .setBean("formatting", new Formatting())
        .setBean("xs", List.of(1, 2, 3))
        .setBean("strings", new String[]{"a", "b"});

    @Test
    void assignAProperty() {
        LValueExpressions$ELExpressions.TITLE.setValue(context, "Sourcegen");
        assertEquals("Sourcegen", book.getTitle());
        assertEquals("Sourcegen", LValueExpressions$ELExpressions.TITLE.getValue(context));
    }

    @Test
    void readOnlyProperties() {
        assertFalse(LValueExpressions$ELExpressions.TITLE.isReadOnly(context));
        assertTrue(LValueExpressions$ELExpressions.CATEGORY.isReadOnly(context));
        assertThrows(PropertyNotWritableException.class,
            () -> LValueExpressions$ELExpressions.CATEGORY.setValue(context, "computing"));
    }

    @Test
    void propertyTypeAndReference() {
        assertEquals(String.class, LValueExpressions$ELExpressions.TITLE.getType(context));
        ValueReference reference = LValueExpressions$ELExpressions.TITLE.getValueReference(context);
        assertEquals(book, reference.getBase());
        assertEquals("title", reference.getProperty());
    }

    @Test
    void assignAnIdentifier() {
        LValueExpressions$ELExpressions.COUNTER.setValue(context, 1L);
        assertEquals(1L, context.getBean("counter"));
        assertEquals(1L, (Object) LValueExpressions$ELExpressions.COUNTER.getValue(context));
    }

    @Test
    void expressionsThatAreNotLValues() {
        assertTrue(LValueExpressions$ELExpressions.NOT_AN_LVALUE.isReadOnly(context));
        assertNull(LValueExpressions$ELExpressions.NOT_AN_LVALUE.getType(context));
        assertThrows(PropertyNotWritableException.class,
            () -> LValueExpressions$ELExpressions.NOT_AN_LVALUE.setValue(context, 1));
    }

    @Test
    void methodExpressions() {
        assertEquals("EL (history)", LValueExpressions$ELExpressions.DESCRIBE.invoke(context, null));
        assertEquals(10d, LValueExpressions$ELExpressions.HALF_PRICE.invoke(context, null));
        assertTrue(LValueExpressions$ELExpressions.HALF_PRICE.isParametersProvided());
        assertFalse(LValueExpressions$ELExpressions.DESCRIBE.isParametersProvided());
        assertEquals("Hi, Denis",
            LValueExpressions$ELExpressions.GREET.invoke(context, new Object[]{"Hi"}));
        assertEquals("integer",
            LValueExpressions$ELExpressions.SELECT_INTEGER.invoke(context, new Object[]{"1"}));
        assertEquals("string",
            LValueExpressions$ELExpressions.SELECT_STRING.invoke(context, new Object[]{"1"}));

        ExpressionFactory factory = ExpressionFactory.newInstance();
        assertEquals("integer", factory.createMethodExpression(context, "${formatting.select}", String.class,
            new Class<?>[]{int.class}).invoke(context, new Object[]{"1"}));
        assertEquals("string", factory.createMethodExpression(context, "${formatting.select}", String.class,
            new Class<?>[]{String.class}).invoke(context, new Object[]{"1"}));
        assertEquals(42, LValueExpressions$ELExpressions.INTEGER_VALUE_OF.invoke(context, new Object[]{"42"}));
        assertEquals(3, LValueExpressions$ELExpressions.LIST_SIZE.invoke(context, null));
    }

    @Test
    void methodInfo() {
        assertEquals("describe", LValueExpressions$ELExpressions.DESCRIBE.getMethodInfo(context).getName());
    }

    @Test
    void variableArityMethodsHandleDirectArrays() {
        assertEquals("1:int[]", LValueExpressions$ELExpressions.PACKED_PRIMITIVE_ARRAY.getValue(context));
        assertEquals("1:java.lang.String[]", LValueExpressions$ELExpressions.PACKED_REFERENCE_ARRAY.getValue(context));
        assertEquals("a", LValueExpressions$ELExpressions.FIRST_STRING.getValue(context));
    }

    @Test
    void generatedRegistryHonorsNullableMethodExpressionExpectations() {
        CompiledExpressionFactory factory = new CompiledExpressionFactory(List.of(new LValueExpressions$ELExpressions()));

        MethodExpression embedded = factory.createMethodExpression(context, "${book.discounted(50)}", null, null);
        assertTrue(embedded.isParametersProvided());
        assertEquals(10d, embedded.invoke(context, null));
        assertThrows(NullPointerException.class,
            () -> factory.createMethodExpression(context, "${book.describe}", String.class, null));
    }

    @Test
    void generatedMethodExpressionsWithProvidedParametersSurviveSerialization() throws Exception {
        MethodExpression copy = roundTrip(LValueExpressions$ELExpressions.HALF_PRICE);

        assertTrue(copy.isParametersProvided());
        assertEquals(10d, copy.invoke(context, null));
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
}
