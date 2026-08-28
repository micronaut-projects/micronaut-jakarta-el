package io.micronaut.el.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the processor generates for an expression: which calls into the runtime the compiled class makes.
 */
class GeneratedSourceTest {

    private static final String PACKAGE = "io.micronaut.el.test.generated";

    private static String source(String expression) {
        return """
            package io.micronaut.el.test.generated;

            import io.micronaut.el.annotation.*;

            @ELEnvironment(variables = {
                @ELVariable(name = "book", type = Book.class),
                @ELVariable(name = "varargs", type = Book.class)
            })
            @ELExpression(value = "%s", expectedType = String.class)
            public class Expressions {
            }

            class Book {
                public String getTitle() { return "title"; }
                public double getUnitPrice() { return 1d; }
                public java.util.List<String> getTags() { return java.util.List.of(); }
                public Object apply(jakarta.el.LambdaExpression lambda) { return lambda.invoke("title"); }
                public double discount(double percent) { return percent; }
                public long count(java.util.function.Predicate<String> predicate) { return getTags().stream().filter(predicate).count(); }
                public String choose(Number first, Number second) { return "assignable"; }
                public String choose(Long first, String second) { return "coercible"; }
            }
            """.formatted(expression.replace("\"", "\\\""));
    }

    /**
     * @return The calls into the runtime the {@code evaluate} method of the generated class makes, by method
     * name, with their count
     */
    private static Map<String, Integer> runtimeCalls(String expression) throws IOException {
        try (JavaParser parser = new JavaParser()) {
            for (JavaFileObject file : parser.generate(PACKAGE + ".Expressions", source(expression))) {
                if (file.getName().endsWith("Expressions$Expression0.class")) {
                    Map<String, Integer> calls = new TreeMap<>();
                    try (InputStream input = file.openInputStream()) {
                        new ClassReader(input.readAllBytes()).accept(new ClassVisitor(Opcodes.ASM9) {
                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                if (!name.equals("evaluate") && !name.startsWith("lambda$")) {
                                    return null;
                                }
                                return new MethodVisitor(Opcodes.ASM9) {
                                    @Override
                                    public void visitMethodInsn(int opcode, String owner, String method, String methodDescriptor, boolean isInterface) {
                                        if (owner.startsWith("io/micronaut/el/runtime/")) {
                                            calls.merge(method, 1, Integer::sum);
                                        }
                                    }
                                };
                            }
                        }, 0);
                    }
                    return calls;
                }
            }
            throw new AssertionError("no class generated");
        }
    }

    @Test
    void aVariableReferencedTwiceIsResolvedOnce() throws IOException {
        Map<String, Integer> calls = runtimeCalls("Book: ${book.title} costs ${book.unitPrice}");
        assertEquals(1, calls.getOrDefault("resolveVariable", 0), calls.toString());
    }

    @Test
    void aDeclaredVariableSkipsTheImportsOfTheIdentifierResolution() throws IOException {
        Map<String, Integer> calls = runtimeCalls("${book.title}");
        assertEquals(1, calls.getOrDefault("resolveVariable", 0), calls.toString());
        assertEquals(0, calls.getOrDefault("resolveIdentifier", 0), calls.toString());
    }

    @Test
    void aCompositeOfKnownTypesIsConcatenatedWithoutTheRuntime() throws IOException {
        Map<String, Integer> calls = runtimeCalls("Book: ${book.title} costs ${book.unitPrice}");
        assertEquals(0, calls.getOrDefault("concat", 0), calls.toString());
        assertEquals(0, calls.getOrDefault("coerceToString", 0), calls.toString());
    }

    @Test
    void theOperatorsOnPrimitivesAreInlined() throws IOException {
        Map<String, Integer> calls = runtimeCalls("${book.unitPrice * 2 + 1 > 10 and not (book.unitPrice == 3) ? -book.unitPrice : 0}");
        assertEquals(Map.of("resolveVariable", 1), calls);
    }

    @Test
    void theOperatorsOnUnknownTypesUseTheRuntime() throws IOException {
        Map<String, Integer> calls = runtimeCalls("${book.tags[0] + 1 > 10}");
        assertEquals(1, calls.getOrDefault("add", 0), calls.toString());
        assertEquals(1, calls.getOrDefault("greaterThanLazy", 0), calls.toString());
    }

    @Test
    void aSemicolonKeepsEveryReferenceResolving() throws IOException {
        Map<String, Integer> calls = runtimeCalls("${book.title; book.title}");
        assertEquals(2, calls.getOrDefault("resolveVariable", 0), calls.toString());
    }

    @Test
    void aLambdaPassedToAStreamOperationIsAJavaLambda() throws IOException {
        Map<String, Integer> calls = runtimeCalls("${book.tags.stream().map(t -> t += book.title).toList()}");
        // the lambda runs within the evaluation, so the variable it refers to is resolved once, outside it
        assertEquals(Map.of("resolveVariable", 1), calls);
    }

    @Test
    void theElementsOfAStreamAreTyped() throws IOException {
        Map<String, Integer> calls = runtimeCalls("${book.tags.stream().filter(t -> t.length() > 1).sorted((a, b) -> b.length() - a.length()).toList()}");
        assertEquals(Map.of("resolveVariable", 1, "coerceToType", 1), calls);
    }

    @Test
    void aLambdaUsedAsAValueIsACompiledLambdaExpression() throws IOException {
        Map<String, Integer> calls = runtimeCalls("${book.apply(t -> t += '!')}");
        assertEquals(1, calls.getOrDefault("lambda1", 0), calls.toString());
        assertEquals(0, calls.getOrDefault("lambdaArgument", 0), calls.toString());
    }

    @Test
    void aLambdaValueResolvesItsFreeIdentifiersWhenInvoked() throws IOException {
        // the lambda is a value invoked later, possibly with another context: its body resolves the variable
        Map<String, Integer> calls = runtimeCalls("${book.apply(t -> t += book.title)}");
        assertEquals(2, calls.getOrDefault("resolveVariable", 0), calls.toString());
    }

    @Test
    void aResultOfTheExpectedTypeIsNotCoerced() throws IOException {
        assertFalse(coerced("Book: ${book.title} costs ${book.unitPrice}"), "a composite is a String");
        assertFalse(coerced("${'Book: ' += book.title}"), "a concatenation is a String");
        assertTrue(coerced("${book.title}"), "a null String coerces to the empty string");
        assertTrue(coerced("${book.unitPrice}"), "a double is not a String");
    }

    /**
     * @return The {@code coerced} flag the generated class passes to its superclass, for a String expected type
     */
    private static boolean coerced(String expression) throws IOException {
        try (JavaParser parser = new JavaParser()) {
            for (JavaFileObject file : parser.generate(PACKAGE + ".Expressions", source(expression))) {
                if (file.getName().endsWith("Expressions$Expression0.class")) {
                    boolean[] coerced = new boolean[1];
                    try (InputStream input = file.openInputStream()) {
                        new ClassReader(input.readAllBytes()).accept(new ClassVisitor(Opcodes.ASM9) {
                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                                if (!name.equals("<init>")) {
                                    return null;
                                }
                                return new MethodVisitor(Opcodes.ASM9) {
                                    @Override
                                    public void visitInsn(int opcode) {
                                        if (opcode == Opcodes.ICONST_1) {
                                            coerced[0] = true;
                                        }
                                    }
                                };
                            }
                        }, 0);
                    }
                    return coerced[0];
                }
            }
            throw new AssertionError("no class generated");
        }
    }

    @Test
    void aLiteralArgumentIsCoercedAtCompilationTime() throws IOException {
        Map<String, Integer> calls = runtimeCalls("${book.discount(10)}");
        assertEquals(Map.of("resolveVariable", 1), calls);
    }

    @Test
    void anAssignableOverloadIsCalledDirectlyBeforeACoercibleOne() throws IOException {
        Map<String, Integer> calls = runtimeCalls("${varargs.choose(1, 1)}");
        assertEquals(0, calls.getOrDefault("invoke", 0), calls.toString());
        assertEquals(1, calls.getOrDefault("resolveVariable", 0), calls.toString());
    }

    @Test
    void aLambdaPassedToAFunctionalInterfaceIsAJavaLambdaWithTypedParameters() throws IOException {
        Map<String, Integer> calls = runtimeCalls("${book.count(t -> t.length() > 2)}");
        assertEquals(Map.of("resolveVariable", 1), calls);
    }
}
