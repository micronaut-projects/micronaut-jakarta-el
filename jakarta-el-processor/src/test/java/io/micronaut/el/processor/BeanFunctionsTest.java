package io.micronaut.el.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The functions declared on a bean: the instance methods it declares are functions, and a call that does not
 * match a function fails the compilation.
 */
class BeanFunctionsTest {

    private static final String PACKAGE = "io.micronaut.el.test.functions";

    private static String source(String expression) {
        return """
            package io.micronaut.el.test.functions;

            import io.micronaut.el.annotation.*;

            @ELEnvironment(
                variables = @ELVariable(name = "book", type = Book.class),
                functions = @ELFunctions(value = PricingService.class, prefix = "pricing")
            )
            @ELExpression(value = "%s", expectedType = Object.class)
            public class Expressions {
            }

            class Book {
                public double getUnitPrice() { return 20d; }
            }

            class PricingService {
                public double quote(Book book, int quantity) { return book.getUnitPrice() * quantity; }
                public static String currency() { return "EUR"; }
            }
            """.formatted(expression);
    }

    private static String failure(String expression) {
        try (JavaParser parser = new JavaParser()) {
            RuntimeException failure = assertThrows(RuntimeException.class,
                () -> parser.generate(PACKAGE + ".Expressions", source(expression)));
            return failure.getMessage();
        }
    }

    @Test
    void anInstanceMethodOfTheBeanIsAFunction() {
        try (JavaParser parser = new JavaParser()) {
            Iterable<? extends JavaFileObject> generated = parser.generate(PACKAGE + ".Expressions",
                source("${pricing:quote(book, 3) += ' ' += pricing:currency()}"));
            List<String> names = StreamSupport.stream(generated.spliterator(), false).map(JavaFileObject::getName).toList();
            assertTrue(names.stream().anyMatch(name -> name.endsWith("Expressions$Expression0.class")), names.toString());
        }
    }

    @Test
    void aCallWithTheWrongNumberOfArgumentsFails() {
        String error = failure("${pricing:quote(book)}");
        assertTrue(error.contains("The function 'pricing:quote' expects 2 argument(s) but 1 were given"), error);
    }

    @Test
    void aCallToAFunctionTheBeanDoesNotDeclareFails() {
        String error = failure("${pricing:discount(book)}");
        assertTrue(error.contains("The function 'pricing:discount' is not declared"), error);
    }

    @Test
    void aCallWithAnUnknownPrefixFails() {
        String error = failure("${sales:quote(book, 3)}");
        assertTrue(error.contains("The function 'sales:quote' is not declared"), error);
    }
}
