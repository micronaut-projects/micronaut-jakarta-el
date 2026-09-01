package io.micronaut.el.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An expression that cannot be compiled fails the compilation of the class declaring it, with the expression,
 * the position and the class in the message, so that it never reaches a runtime.
 */
class CompilationErrorsTest {

    private static final String PACKAGE = "io.micronaut.el.test.errors";

    private static String source(String annotations) {
        return """
            package io.micronaut.el.test.errors;

            import io.micronaut.el.annotation.*;

            @ELEnvironment(variables = @ELVariable(name = "book", type = Book.class), imports = Math.class)
            %s
            public class Expressions {
            }

            class Book {
                public String getTitle() { return "title"; }
                public double getPrice() { return 1d; }
                public String describe() { return "a book"; }
            }
            """.formatted(annotations);
    }

    private static String compile(String annotations) {
        try (JavaParser parser = new JavaParser()) {
            RuntimeException failure = assertThrows(RuntimeException.class,
                () -> parser.generate(PACKAGE + ".Expressions", source(annotations)));
            return failure.getMessage();
        }
    }

    @Test
    void anOmittedExpectedTypeWithAnUndeclaredIdentifierIsReported() {
        String error = compile("@ELExpression(\"${ customer.age }\")");
        assertTrue(error.contains("Cannot infer the expectedType"), error);
        assertTrue(error.contains("the identifier 'customer' is not declared"), error);
        assertTrue(error.contains("@ELVariable(name = \"customer\""), error);
        assertTrue(error.contains("Object.class accepts any result"), error);
    }

    @Test
    void aSyntaxErrorIsReportedWithTheExpressionAndThePosition() {
        String error = compile("@ELExpression(\"${ book.title + }\")");
        assertTrue(error.contains("Expressions.java"), error);
        assertTrue(error.contains("error:"), error);
        assertTrue(error.contains("in the expression [${ book.title + }] at the position"), error);
    }

    @Test
    void anUnterminatedExpressionIsReported() {
        String error = compile("@ELExpression(\"${ book.title\")");
        assertTrue(error.contains("in the expression [${ book.title] at the position"), error);
    }

    @Test
    void anInvalidOperatorSequenceIsReported() {
        String error = compile("@ELExpression(\"${ book.price * / 2 }\")");
        assertTrue(error.contains("[${ book.price * / 2 }] at the position"), error);
    }

    @Test
    void aMethodExpressionMustBeASingleEvalExpression() {
        String error = compile("@ELMethodExpression(\"Hello ${book.describe}\")");
        assertTrue(error.contains("Only a single eval-expression can be used as a method expression"), error);
    }

    @Test
    void anUnknownStaticFieldOfAnImportedClassIsReported() {
        String error = compile("@ELExpression(\"${ Math.PIE }\")");
        assertTrue(error.contains("java.lang.Math does not declare the public static field 'PIE'"), error);
    }

    @Test
    void anAssignmentToSomethingThatIsNotAnLvalueIsReported() {
        String error = compile("@ELExpression(\"${ 1 = 2 }\")");
        assertTrue(error.contains("The left side of an assignment must be an lvalue"), error);
    }

    @Test
    void aMapConstructionWithoutAValueIsReported() {
        String error = compile("@ELExpression(\"${ {'a': 1, 'b'} }\")");
        assertTrue(error.contains("cannot mix set elements and map entries in the expression [${ {'a': 1, 'b'} }] at the position"), error);
    }

    @Test
    void ambiguousOverloadsOfAStaticImportAreNotDiscarded() {
        String source = """
            package io.micronaut.el.test.errors;

            import io.micronaut.el.annotation.*;
            import java.io.Serializable;

            @ELEnvironment(staticImports = {First.class, Second.class})
            @ELExpression(value = "${ambiguous('x')}", expectedType = String.class)
            public class Expressions {
            }

            class First {
                public static String ambiguous(Comparable<?> value) { return "comparable"; }
                public static String ambiguous(Serializable value) { return "serializable"; }
            }

            class Second {
                public static String ambiguous(Object value) { return "object"; }
            }
            """;
        try (JavaParser parser = new JavaParser()) {
            RuntimeException failure = assertThrows(RuntimeException.class,
                () -> parser.generate(PACKAGE + ".Expressions", source));
            assertTrue(failure.getMessage().contains("imported static method 'ambiguous' is ambiguous"),
                failure.getMessage());
        }
    }

    @Test
    void aLambdaWithUnrelatedFunctionalTargetsIsRejectedAsAmbiguous() {
        String source = """
            package io.micronaut.el.test.errors;

            import io.micronaut.el.annotation.*;
            import java.util.function.Function;
            import java.util.function.Predicate;

            @ELEnvironment(variables = @ELVariable(name = "varargs", type = Router.class))
            @ELExpression(value = "${varargs.route(value -> value)}", expectedType = String.class)
            public class Expressions {
            }

            class Router {
                public String route(Predicate<String> predicate) { return "predicate"; }
                public String route(Function<String, String> function) { return "function"; }
            }
            """;
        try (JavaParser parser = new JavaParser()) {
            RuntimeException failure = assertThrows(RuntimeException.class,
                () -> parser.generate(PACKAGE + ".Expressions", source));
            assertTrue(failure.getMessage().contains("is ambiguous for the lambda expression"),
                failure.getMessage());
        }
    }

    @Test
    void aMemberTheTypedVariableDoesNotDeclareIsAWarning() {
        try (JavaParser parser = new JavaParser()) {
            Iterable<? extends JavaFileObject> generated = parser.generate(PACKAGE + ".Expressions",
                source("@ELExpression(\"${ book.titel }\") @ELExpression(\"${ book.shout('x') }\")"));
            assertTrue(generated.iterator().hasNext());
            List<String> warnings = parser.getDiagnosticCollector().getDiagnostics().stream()
                .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.WARNING
                    || diagnostic.getKind() == Diagnostic.Kind.MANDATORY_WARNING)
                .map(diagnostic -> diagnostic.getMessage(null))
                .filter(message -> message.contains("does not declare"))
                .toList();
            assertEquals(2, warnings.size(), warnings.toString());
            assertTrue(warnings.get(0).contains("Book does not declare the property 'titel'"), warnings.toString());
            assertTrue(warnings.get(1).contains("Book does not declare the method 'shout(1 argument(s))'"), warnings.toString());
        }
    }

    @Test
    void aValidExpressionCompilesIntoTheRegistryOfTheClass() {
        try (JavaParser parser = new JavaParser()) {
            Iterable<? extends JavaFileObject> generated = parser.generate(PACKAGE + ".Expressions",
                source("@ELExpression(value = \"${ book.title += ' costs ' += book.price }\", expectedType = String.class)"));
            List<String> names = StreamSupport.stream(generated.spliterator(), false)
                .map(JavaFileObject::getName)
                .toList();
            assertTrue(names.stream().anyMatch(name -> name.endsWith("Expressions$Expression0.class")), names.toString());
            assertTrue(names.stream().anyMatch(name -> name.endsWith("Expressions$ELExpressions.class")), names.toString());
        }
    }
}
