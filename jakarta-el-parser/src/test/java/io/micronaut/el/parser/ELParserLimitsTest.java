package io.micronaut.el.parser;

import io.micronaut.el.parser.ast.ELNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bounds of the parser accept the ordinary expressions both reference implementations accept, count only the
 * constructs that really nest, and can be changed through {@link ELParserOptions}.
 */
class ELParserLimitsTest {

    @Test
    void ordinaryExpressionsAreWithinTheDefaultLimits() {
        for (String expression : ordinaryExpressions()) {
            assertNotNull(ELParser.parse(expression), expression.substring(0, 30));
        }
    }

    @Test
    void aChainDoesNotNest() {
        // a chain twice as long as the nesting limit is parsed: only the tree height bounds it
        int terms = ELParser.DEFAULT_MAX_DEPTH * 2;
        assertNotNull(ELParser.parse("${" + "1+".repeat(terms) + "1}"));
        assertNotNull(ELParser.parse("${" + "true and ".repeat(terms) + "true}"));
        assertNotNull(ELParser.parse("${" + "true or ".repeat(terms) + "true}"));
        assertNotNull(ELParser.parse("${" + "'a' += ".repeat(terms) + "'a'}"));
        assertNotNull(ELParser.parse("${s" + ".trim()".repeat(terms) + "}"));
        assertNotNull(ELParser.parse("${a" + ".b".repeat(terms) + "}"));
        assertNotNull(ELParser.parse("${a" + "[0]".repeat(terms) + "}"));
        assertNotNull(ELParser.parse("${" + "1;".repeat(terms) + "1}"));
        assertNotNull(ELParser.parse("${" + "1 < ".repeat(terms) + "1}"));
        assertNotNull(ELParser.parse("${" + "1 == ".repeat(terms) + "1}"));
        assertNotNull(ELParser.parse("${" + "2*".repeat(terms) + "1}"));
        assertNotNull(ELParser.parse("${((x)->x)" + "(1)".repeat(terms) + "}"));
        // a chain nested inside the deepest allowed parenthesis still counts as one level
        int depth = ELParser.DEFAULT_MAX_DEPTH - 1;
        assertNotNull(ELParser.parse("${" + "(".repeat(depth) + "1+".repeat(terms) + "1" + ")".repeat(depth) + "}"));
    }

    @Test
    void realNestingIsStillBounded() {
        int depth = ELParser.DEFAULT_MAX_DEPTH + 1;
        for (String nested : List.of(
            "${" + "(".repeat(depth) + "1" + ")".repeat(depth) + "}",
            "${" + "!".repeat(depth) + "true}",
            "${" + "[".repeat(depth) + "]".repeat(depth) + "}",
            "${" + "f(".repeat(depth) + "1" + ")".repeat(depth) + "}",
            "${" + "a=".repeat(depth) + "1}",
            "${" + "x->".repeat(depth) + "1}",
            "${" + "true?".repeat(depth) + "1" + ":2".repeat(depth) + "}",
            "${" + "(1+".repeat(depth) + "1" + ")".repeat(depth) + "}")) {
            ELParsingException e = assertThrows(ELParsingException.class, () -> ELParser.parse(nested));
            assertTrue(e.getMessage().contains("nests more than " + ELParser.DEFAULT_MAX_DEPTH + " levels deep"),
                e.getMessage());
        }
    }

    @Test
    void aTreeTallerThanTheLimitIsRejectedWithoutOverflowingTheStack() {
        int terms = ELParserOptions.DEFAULT_MAX_TREE_HEIGHT + 10;
        ELParsingException e = assertThrows(ELParsingException.class,
            () -> ELParser.parse("${" + "1+".repeat(terms) + "1}"));
        assertTrue(e.getMessage().contains("cannot be more than " + ELParserOptions.DEFAULT_MAX_TREE_HEIGHT
            + " nodes high"), e.getMessage());
        // pathological input is rejected on a thread with a small stack as well
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread thread = new Thread(null, () -> {
            try {
                ELParser.parse("${" + "1+".repeat(60_000) + "1}");
            } catch (Throwable t) {
                failure.set(t);
            }
        }, "small-stack", 256 * 1024);
        thread.start();
        join(thread);
        assertInstanceOf(ELParsingException.class, failure.get());
    }

    @Test
    void everyLimitIsConfigurable() {
        ELParserOptions small = ELParserOptions.defaults()
            .withMaxLength(10).withMaxTokens(5).withMaxDepth(2).withMaxTreeHeight(3);
        assertThrows(ELParsingException.class, () -> ELParser.parse("x".repeat(11), small));
        assertNotNull(ELParser.parse("x".repeat(10), small));
        assertThrows(ELParsingException.class, () -> ELParser.parse("${1+1+1}", small));
        assertThrows(ELParsingException.class, () -> ELParser.parse("${((1))}",
            ELParserOptions.defaults().withMaxDepth(2)));
        assertNotNull(ELParser.parse("${(1)}", ELParserOptions.defaults().withMaxDepth(2)));
        assertThrows(ELParsingException.class, () -> ELParser.parse("${1+1+1}",
            ELParserOptions.defaults().withMaxTreeHeight(3)));
        assertNotNull(ELParser.parse("${1+1}", ELParserOptions.defaults().withMaxTreeHeight(3)));
        assertThrows(ELParsingException.class, () -> ELParser.parseEval("${1+1+1}",
            ELParserOptions.defaults().withMaxTreeHeight(3)));

        ELParserOptions large = ELParserOptions.defaults()
            .withMaxLength(4_000_000).withMaxTokens(1_000_000).withMaxTreeHeight(10_000);
        String longText = "x".repeat(2_000_000) + "${1}";
        assertThrows(ELParsingException.class, () -> ELParser.parse(longText));
        assertInstanceOf(ELNode.Composite.class, ELParser.parse(longText, large));
        String longChain = "${" + "1+".repeat(5_000) + "1}";
        assertThrows(ELParsingException.class, () -> ELParser.parse(longChain));
        assertNotNull(ELParser.parse(longChain, large));
    }

    @Test
    void theOptionsAreReadFromProperties() {
        Properties properties = new Properties();
        properties.setProperty(ELParserOptions.MAX_LENGTH_PROPERTY, "100");
        properties.setProperty(ELParserOptions.MAX_TOKENS_PROPERTY, "50");
        properties.setProperty(ELParserOptions.MAX_DEPTH_PROPERTY, "7");
        properties.setProperty(ELParserOptions.MAX_TREE_HEIGHT_PROPERTY, "2_000");
        ELParserOptions options = ELParserOptions.fromProperties(properties);
        assertEquals(100, options.maxLength());
        assertEquals(50, options.maxTokens());
        assertEquals(7, options.maxDepth());
        assertEquals(2000, options.maxTreeHeight());
        assertEquals(ELParserOptions.defaults(), ELParserOptions.fromResolver(name -> null));
        assertEquals(options, ELParserOptions.defaults().withMaxLength(100).withMaxTokens(50).withMaxDepth(7)
            .withMaxTreeHeight(2000));
        properties.setProperty(ELParserOptions.MAX_DEPTH_PROPERTY, "deep");
        assertThrows(IllegalArgumentException.class, () -> ELParserOptions.fromProperties(properties));
        assertThrows(IllegalArgumentException.class, () -> ELParserOptions.defaults().withMaxDepth(0));
        assertNull(System.getProperty(ELParserOptions.MAX_DEPTH_PROPERTY));
        assertEquals(ELParserOptions.defaults(), ELParserOptions.fromSystemProperties());
    }

    /**
     * Expressions that Expressly and Tomcat both accept, and that the original limits rejected.
     */
    static List<String> ordinaryExpressions() {
        return List.of(
            "x".repeat(17_000) + "${1}",
            "${[" + "1,".repeat(599) + "1]}",
            "${1} ".repeat(300),
            "${" + "1+".repeat(149) + "1}",
            "${" + "true and ".repeat(109) + "true}",
            "${" + "'a' += ".repeat(119) + "'a'}",
            "${' a '" + ".trim()".repeat(120) + "}");
    }

    private static void join(Thread thread) {
        try {
            thread.join(60_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
        assertFalse(thread.isAlive(), "the parse did not complete");
    }
}
