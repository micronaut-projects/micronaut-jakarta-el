package io.micronaut.el.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The expressions declared on a method see its parameters as typed variables, and the environment declared on
 * the member.
 */
class MemberEnvironmentTest {

    private static List<String> warnings(JavaParser parser) {
        return parser.getDiagnosticCollector().getDiagnostics().stream()
            .filter(diagnostic -> diagnostic.getKind() == Diagnostic.Kind.WARNING
                || diagnostic.getKind() == Diagnostic.Kind.MANDATORY_WARNING)
            .map(diagnostic -> diagnostic.getMessage(null))
            .filter(message -> message.contains("does not declare"))
            .toList();
    }

    @Test
    void theParametersOfAMethodAreTypedVariablesOfItsExpressions() {
        try (JavaParser parser = new JavaParser()) {
            parser.generate("io.micronaut.el.test.members.Service", """
                package io.micronaut.el.test.members;

                import io.micronaut.el.annotation.*;

                public class Service {

                    @ELExpression(value = "${ customer.agee >= 18 }", expectedType = Boolean.class)
                    public void register(Customer customer) {
                    }
                }

                class Customer {
                    public int getAge() { return 1; }
                }
                """);
            List<String> warnings = warnings(parser);
            assertEquals(1, warnings.size(), warnings.toString());
            assertTrue(warnings.get(0).contains("Customer does not declare the property 'agee'"), warnings.toString());
        }
    }

    private static final String CUSTOMER_AND_RULES = """

        class Customer {
            public int getAge() { return 1; }
            public String getCountry() { return "DE"; }
        }

        class Rules {
            public static boolean adult(int age) { return age >= 18; }
        }
        """;

    @Test
    void theEnvironmentOfAMemberAppliesToItsExpressions() {
        try (JavaParser parser = new JavaParser()) {
            parser.generate("io.micronaut.el.test.members.Service", """
                package io.micronaut.el.test.members;

                import io.micronaut.el.annotation.*;

                public class Service {

                    @ELEnvironment(functions = @ELFunctions(value = Rules.class, prefix = "fn"), imports = java.util.Locale.class)
                    @ELExpression(value = "${ fn:adult(customer.age) && customer.country == Locale.GERMANY.country }", expectedType = Boolean.class)
                    public void register(Customer customer) {
                    }
                }
                """ + CUSTOMER_AND_RULES);
            assertTrue(warnings(parser).isEmpty(), warnings(parser).toString());
        }
    }

    @Test
    void theEnvironmentOfAMemberDoesNotLeakToAnotherMember() {
        try (JavaParser parser = new JavaParser()) {
            RuntimeException failure = assertThrows(RuntimeException.class, () -> parser.generate("io.micronaut.el.test.members.Service", """
                package io.micronaut.el.test.members;

                import io.micronaut.el.annotation.*;

                public class Service {

                    @ELEnvironment(functions = @ELFunctions(value = Rules.class, prefix = "fn"))
                    @ELExpression(value = "${ fn:adult(customer.age) }", expectedType = Boolean.class)
                    public void register(Customer customer) {
                    }

                    @ELExpression(value = "${ fn:adult(customer.age) || false }", expectedType = Boolean.class)
                    public void unrelated(Customer customer) {
                    }
                }
                """ + CUSTOMER_AND_RULES));
            assertTrue(failure.getMessage().contains("The function 'fn:adult' is not declared"), failure.getMessage());
        }
    }
}
