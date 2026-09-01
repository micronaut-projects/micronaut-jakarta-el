package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.micronaut.el.test.SpecExamplesExpressions$ELExpressions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpecExamplesExpressionsTest {

    private final ELContext context = new CompiledELContext()
        .setBean("item", new Inventory("A-1", 5, 10L, 9.99d, 1.5f, true, Suit.HEART))
        .setBean("prices", Map.of("widget", 2.5d));

    @Test
    void literalAndCompositeExpressions() {
        assertEquals("plain text", value(LITERAL_TEXT));
        assertEquals("${not an expression}", value(ESCAPED_EXPRESSION));
        assertEquals("A-1 has 5 in HEART", value(COMPOSITE_TEXT));
    }

    @Test
    void literals() {
        assertEquals(42L, value(INTEGER_LITERAL));
        assertEquals(4.2d, value(FLOATING_POINT_LITERAL));
        assertEquals(1000d, value(EXPONENT_LITERAL));
        assertEquals("it's", value(STRING_LITERAL_QUOTE));
        assertEquals("double \"quoted\"", value(STRING_LITERAL_DOUBLE_QUOTED));
        assertEquals(true, value(BOOLEAN_LITERAL));
        assertNull(value(NULL_LITERAL));
    }

    @Test
    void propertiesMethodsAndArithmetic() {
        assertEquals("A-1", value(BRACKET_PROPERTY));
        assertEquals("sale", value(LIST_INDEX));
        assertEquals(2.5d, value(MAP_KEY));
        assertEquals(2.5d, value(MAP_PROPERTY));
        assertEquals(3, value(METHOD_ON_LIST));
        assertEquals(5 * 9.99d - 10d / 2, value(MIXED_ARITHMETIC));
        assertEquals(2.5d, value(DIV_WORD));
        assertEquals(2L, value(MOD_WORD));
        assertEquals(-6L, value(NEGATED_PARENTHESIS));
        assertEquals(15L, value(STRING_ARITHMETIC));
        assertEquals("105", value(STRING_CONCATENATION_OPERATOR));
        assertEquals(1L, value(NULL_ARITHMETIC));
    }

    @Test
    void relationalLogicalEmptyAndConditional() {
        assertEquals(true, value(EQ_WORD));
        assertEquals(false, value(NE_WORD));
        assertEquals(true, value(STRING_LESS_THAN));
        assertEquals(true, value(ENUM_EQUALS_STRING));
        assertEquals(true, value(ENUM_EQUALS_CONSTANT));
        assertEquals(true, value(COERCED_EQUALITY));
        assertEquals(true, value(LOGICAL_WORDS));
        assertEquals(false, value(EMPTY_STRING));
        assertEquals(false, value(EMPTY_LIST));
        assertEquals(false, value(EMPTY_MAP));
        assertEquals("many", value(CONDITIONAL));
        assertEquals(5, value(CONDITIONAL_MIXED_TYPES));
    }

    @Test
    void assignmentAndSemicolon() {
        assertEquals(11L, value(ASSIGNMENT_AND_SEMICOLON));
    }

    @Test
    void collectionsAndStaticReferences() {
        assertEquals(List.of(1L, 2L, 3L), value(LIST_CONSTRUCTION));
        assertEquals(Set.of(1L, 2L), value(SET_CONSTRUCTION));
        assertEquals(Map.of("one", 1L, "two", 2L), value(MAP_CONSTRUCTION));
        assertEquals(List.of("A-1", 5), value(LIST_OF_VALUES));
        assertEquals(Suit.SPADE, value(STATIC_FIELD));
        assertEquals(Suit.CLUB, value(STATIC_METHOD));
        assertEquals(Integer.MAX_VALUE, value(JAVA_LANG_STATIC_FIELD));
        assertEquals(7, value(JAVA_LANG_STATIC_METHOD));
    }

    @Test
    void streamsAndOptionals() {
        assertEquals(List.of("b", "new", "sale"), value(STREAM_SORTED));
        assertEquals(8L, value(STREAM_SUM));
        assertEquals(8d / 3, value(STREAM_AVERAGE));
        assertEquals(3L, value(STREAM_COUNT));
        assertEquals(List.of("new", "sale"), value(STREAM_LIMIT));
        assertEquals(List.of("sale", "b"), value(STREAM_SUBSTREAM));
        assertEquals("new", value(OPTIONAL_OR_ELSE));
        assertEquals(true, value(STREAM_ALL_MATCH));
        assertEquals(true, value(STREAM_NONE_MATCH));
        assertEquals("b", value(STREAM_MIN));
        assertEquals("new,sale,b", value(STREAM_REDUCE));
        assertEquals(List.of(1L, 2L, 3L), value(LITERAL_STREAM_SORTED));
        assertEquals(List.of(1L, 2L, 3L), value(STREAM_FLAT_MAP));
    }

    private Object value(ValueExpression expression) {
        return expression.getValue(context);
    }
}
