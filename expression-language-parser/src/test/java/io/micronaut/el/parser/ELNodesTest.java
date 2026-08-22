package io.micronaut.el.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ELNodesTest {

    private static String canonical(String expression) {
        return ELNodes.canonical(ELParser.parse(expression));
    }

    @Test
    void whitespaceDelimitersAndOperatorAliasesDoNotMatter() {
        assertEquals(canonical("${A+B+C}"), canonical("${ A + B\t+\t\tC\t}"));
        assertEquals(canonical("${A < B}"), canonical("${A lt B}"));
        assertEquals(canonical("${A and not B}"), canonical("${A && !B}"));
        assertEquals(canonical("${A}"), canonical("#{A}"));
        assertEquals(canonical("${a.b}"), canonical("${a['b']}"));
    }

    @Test
    void operandOrderAndPrecedenceMatter() {
        assertNotEquals(canonical("${A + B}"), canonical("${B + A}"));
        assertNotEquals(canonical("${A + B * C}"), canonical("${(A + B) * C}"));
        assertEquals("${((A+B)*C)}", canonical("${(A + B) * C}"));
    }

    @Test
    void literalTextEscapesWhatWouldOtherwiseStartAnExpression() {
        assertEquals("a\\${b}", canonical("a\\${b}"));
        assertEquals("text ${x}", canonical("text ${x}"));
    }
}
