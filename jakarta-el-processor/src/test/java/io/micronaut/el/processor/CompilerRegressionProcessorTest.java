package io.micronaut.el.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import io.micronaut.el.processor.visitor.ELExpressionVisitor;
import io.micronaut.el.processor.visitor.ELFunctionVisitor;
import io.micronaut.inject.visitor.TypeElementVisitor;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressions of the processor that only the compilation itself shows: where the failures and the warnings are
 * reported, and how the visitors take part in an incremental build. The interpreted counterpart of the
 * rejected expressions, which no generated expression can exist for, is
 * {@code CompilerRegressionInterpretedTest#whatTheCompilerRejectsFailsWhenInterpreted}.
 */
class CompilerRegressionProcessorTest {

    private static final String PACKAGE = "io.micronaut.el.test.regression";

    private static final String BOOK = """
        class Book {
            public String getTitle() { return "title"; }
            public String over(Integer value) { return "integer"; }
            public String over(String value) { return "string"; }
            public String one(Number value) { return "number"; }
        }
        """;

    private static List<Diagnostic<?>> diagnostics(String source, Diagnostic.Kind kind) {
        try (JavaParser parser = new JavaParser()) {
            try {
                parser.generate(PACKAGE + ".Expressions", source);
            } catch (RuntimeException e) {
                // a failed compilation, whose diagnostics are read below
            }
            return parser.getDiagnosticCollector().getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == kind)
                .<Diagnostic<?>>map(diagnostic -> diagnostic)
                .toList();
        }
    }

    @Test
    void everyFailingExpressionIsReportedOnTheMemberDeclaringIt() {
        String source = """
            package io.micronaut.el.test.regression;

            import io.micronaut.el.annotation.*;

            @ELEnvironment(variables = @ELVariable(name = "book", type = Book.class))
            public class Expressions {

                @ELExpression(value = "${fn:missing(1)}", expectedType = Object.class)
                public String field;

                @ELExpression(value = "${1 +}", expectedType = Object.class)
                public void method() {
                }
            }
            """ + BOOK;
        List<Diagnostic<?>> errors = diagnostics(source, Diagnostic.Kind.ERROR).stream()
            .filter(error -> error.getMessage(null).contains("cannot be compiled"))
            .toList();
        assertEquals(2, errors.size(), errors.toString());
        // the line of the field, then the line of the method, not the line of the class
        assertEquals(9, errors.get(0).getLineNumber(), errors.get(0).toString());
        assertTrue(errors.get(0).getMessage(null).contains("${fn:missing(1)}"), errors.get(0).getMessage(null));
        assertEquals(12, errors.get(1).getLineNumber(), errors.get(1).toString());
        assertTrue(errors.get(1).getMessage(null).contains("${1 +}"), errors.get(1).getMessage(null));
    }

    @Test
    void aMethodWhoseSelectionIsDeferredIsNotReportedAsUndeclared() {
        String source = """
            package io.micronaut.el.test.regression;

            import io.micronaut.el.annotation.*;

            @ELEnvironment(variables = @ELVariable(name = "book", type = Book.class))
            @ELExpression(value = "${book.over(null)}", expectedType = Object.class)
            @ELExpression(value = "${book.over(book.title)}", expectedType = Object.class)
            @ELExpression(value = "${book.one(book)}", expectedType = Object.class)
            @ELExpression(value = "${book.missing(1)}", expectedType = Object.class)
            public class Expressions {
            }
            """ + BOOK;
        List<String> warnings = diagnostics(source, Diagnostic.Kind.WARNING).stream()
            .map(diagnostic -> diagnostic.getMessage(null))
            .filter(message -> message.contains("does not declare"))
            .toList();
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(warnings.get(0).contains("'missing(1 argument(s))'"), warnings.toString());
    }

    @Test
    void theVisitorsAreAggregating() {
        // the expressions of a class call the functions of any other class, so a change to one of those must
        // recompile them: neither visitor is isolating, and both run in the same processor, sharing its context
        assertEquals(TypeElementVisitor.VisitorKind.AGGREGATING, new ELExpressionVisitor().getVisitorKind());
        assertEquals(TypeElementVisitor.VisitorKind.AGGREGATING, new ELFunctionVisitor().getVisitorKind());
    }

    @Test
    void aFunctionDiscoveredInOneCompilationIsNotSeenByAnother() throws IOException {
        String declaring = """
            package io.micronaut.el.test.regression;

            import io.micronaut.el.annotation.*;

            public class Fns {
                @ELFunction
                public static String shout(String value) { return value.toUpperCase(); }
            }
            """;
        try (JavaParser parser = new JavaParser()) {
            parser.generate(PACKAGE + ".Fns", declaring);
        }
        String calling = """
            package io.micronaut.el.test.regression;

            import io.micronaut.el.annotation.*;

            @ELExpression(value = "${shout('a')}", expectedType = Object.class)
            public class Expressions {
            }
            """;
        // a compilation without the class declaring the function does not see the function another compilation
        // discovered, which a static shared between compilations kept: the call stays dynamic
        try (JavaParser parser = new JavaParser()) {
            for (JavaFileObject file : parser.generate(PACKAGE + ".Expressions", calling)) {
                if (file.getName().endsWith(".class")) {
                    try (InputStream input = file.openInputStream()) {
                        String bytes = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
                        assertFalse(bytes.contains("io/micronaut/el/test/regression/Fns"), file.getName());
                    }
                }
            }
        }
    }
}
