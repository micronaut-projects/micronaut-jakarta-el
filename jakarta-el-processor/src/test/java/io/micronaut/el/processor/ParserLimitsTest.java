package io.micronaut.el.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The limits of the parser are read at compile time from the annotation processor options named as the system
 * properties of the runtime parser: an expression beyond a lowered limit fails the compilation, and compiles with
 * the default limits. javac only accepts a key of {@code -A} made of Java identifiers, and rejects the command line
 * {@code -Amicronaut.el.parser.max-length=20} before any processor runs, so these pass the camel case aliases,
 * {@code -Amicronaut.el.parser.maxLength=20}; KSP takes the hyphenated names as well. The interpreted counterparts, the runtime {@code ELParserOptions} with the same limits
 * rejecting the same text, are in {@code io.micronaut.el.interpreter.ParserLimitsInterpretedTest}.
 */
class ParserLimitsTest {

    private static final String CLASS_NAME = "io.micronaut.el.test.limits.Expressions";
    static final String LONG_TEXT = "${'" + "x".repeat(30) + "'}";
    static final String MANY_TOKENS = "${1+1+1+1+1}";
    static final String DEEP = "${((1))}";
    static final String SHALLOW = "${(1+1)}";
    static final String DEEP_METHOD = "#{bean.join(((1)))}";

    private static String source(String annotation) {
        return """
            package io.micronaut.el.test.limits;

            import io.micronaut.el.annotation.*;

            @ELEnvironment(variables = @ELVariable(name = "bean", type = Bean.class))
            %s
            public class Expressions {
            }

            class Bean {
                public String join(Object value) { return String.valueOf(value); }
            }
            """.formatted(annotation);
    }

    private static String value(String expression) {
        return "@ELExpression(value = \"" + expression + "\", expectedType = Object.class, name = \"limited\")";
    }

    private static String failure(Map<String, String> options, String annotation) {
        try (JavaParser parser = parser(options)) {
            RuntimeException failure = assertThrows(RuntimeException.class,
                () -> parser.generate(CLASS_NAME, source(annotation)));
            return failure.getMessage();
        }
    }

    private static JavaParser parser(Map<String, String> options) {
        return new JavaParser() {
            @Override
            protected Set<String> getCompilerOptions() {
                Set<String> compilerOptions = new LinkedHashSet<>(super.getCompilerOptions());
                options.forEach((name, value) -> compilerOptions.add("-A" + name + "=" + value));
                return compilerOptions;
            }
        };
    }

    private static Object evaluate(String annotation, String expression) throws Exception {
        GeneratedExpressions generated = GeneratedExpressions.of(CLASS_NAME, source(annotation));
        ELContext context = new CompiledELContext();
        return generated.registry().createValueExpression(context, expression, Object.class).getValue(context);
    }

    @Test
    void anExpressionLongerThanALoweredMaxLengthFailsTheCompilation() throws Exception {
        String error = failure(Map.of("micronaut.el.parser.maxLength", "20"), value(LONG_TEXT));
        assertTrue(error.contains("The expression '" + LONG_TEXT + "' cannot be compiled"), error);
        assertTrue(error.contains("An expression cannot exceed 20 characters"), error);
        assertTrue(error.contains("micronaut.el.parser.max-length"), error);
        // the default limit accepts it
        assertEquals("x".repeat(30), evaluate(value(LONG_TEXT), LONG_TEXT));
    }

    @Test
    void anExpressionWithMoreTokensThanALoweredMaxTokensFailsTheCompilation() throws Exception {
        String error = failure(Map.of("micronaut.el.parser.maxTokens", "8"), value(MANY_TOKENS));
        assertTrue(error.contains("An expression cannot exceed 8 tokens"), error);
        assertEquals(5L, evaluate(value(MANY_TOKENS), MANY_TOKENS));
    }

    @Test
    void anExpressionNestedDeeperThanALoweredMaxDepthFailsTheCompilation() throws Exception {
        String error = failure(Map.of("micronaut.el.parser.maxDepth", "2"), value(DEEP));
        assertTrue(error.contains("The expression nests more than 2 levels deep"), error);
        assertTrue(error.contains("micronaut.el.parser.max-depth"), error);
        assertEquals(1L, evaluate(value(DEEP), DEEP));
        // within the lowered limit, the expression compiles
        try (JavaParser parser = parser(Map.of("micronaut.el.parser.maxDepth", "2"))) {
            parser.generate(CLASS_NAME, source(value(SHALLOW)));
        }
    }

    @Test
    void aTreeTallerThanALoweredMaxTreeHeightFailsTheCompilation() {
        String error = failure(Map.of("micronaut.el.parser.maxTreeHeight", "3"), value(MANY_TOKENS));
        assertTrue(error.contains("The syntax tree of an expression cannot be more than 3"), error);
    }

    @Test
    void aMethodExpressionIsHeldToTheSameLimits() {
        String error = failure(Map.of("micronaut.el.parser.maxDepth", "2"),
            "@ELMethodExpression(value = \"" + DEEP_METHOD + "\", expectedReturnType = Object.class, name = \"limited\")");
        assertTrue(error.contains("The expression nests more than 2 levels deep"), error);
    }

    @Test
    void javacRejectsTheHyphenatedNameItselfSoTheAliasExists() {
        String error = failure(Map.of("micronaut.el.parser.max-length", "20"), value(SHALLOW));
        assertTrue(error.contains("is not a dot-separated sequence of identifiers"), error);
    }

    @Test
    void anInvalidOptionIsReported() {
        String error = failure(Map.of("micronaut.el.parser.maxLength", "many"), value(SHALLOW));
        assertTrue(error.contains("micronaut.el.parser.max-length must be an integer"), error);
    }
}
