/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.annotation.processing.test;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.ELExpressionSource;
import io.micronaut.el.ELSandbox;
import io.micronaut.el.interpreter.InterpretingELExpressionParser;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodNotFoundException;
import jakarta.el.ValueExpression;
import jakarta.el.ValueReference;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The compiled path and the interpreted one must give the same answer for the same expression: the compiler
 * and the interpreter are two translations of one specification, and the only claim that keeps them honest is
 * that they agree.
 *
 * <p>The expressions are compiled here by the annotation processor itself, which writes them as bytecode
 * rather than as Java source, so the calls the generated methods make are the ones the writer emitted with no
 * compiler of the language to bind them. That is what a Groovy or a Kotlin build gets.</p>
 *
 * <p>This test lives in the package of the in-memory compiler because the class loader over its output is
 * package private.</p>
 */
class CompiledVersusInterpretedTest {

    private static final String PACKAGE = "el.compiled";
    private static final int RANDOM_EXPRESSIONS = 120;

    private final ExpressionFactory interpreted =
        new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser());

    @Test
    void theTwoPathsAgreeOnTheExpressionsOfTheLanguage() throws Exception {
        List<Case> cases = new ArrayList<>();
        for (String expression : List.of(
            "${book.title}", "${book.unitPrice * 2}", "${book.pages gt 100}", "${book.stock['x']}",
            "${book.codes[0]}", "${book.codes.length}", "${book.tags.size()}", "${book.discount(10)}",
            "${book.available ? 'yes' : 'no'}", "${book.tags.stream().toList()}",
            "${book.sizes.stream().sum()}", "${book.tags.stream().filter(t -> t != 'a').toList()}",
            "${book.sizes.stream().map(s -> s * 2).toList()}", "${book.sizes.stream().max()}",
            "${empty book.tags}", "${-book.unitPrice}", "${book.title += '!'}",
            "${(1 ; book.title)}", "${book.title ; 1}", "text ${book.title} tail",
            "${book.title}${book.pages}", "#{book.title}", "${[book.pages, book.isbn]}",
            "${{book.title : book.pages}}", "${Integer.MAX_VALUE}", "${Math.max(1, 2)}")) {
            for (String expectedType : List.of("Object", "String", "Boolean")) {
                cases.add(new Case(expression, expectedType));
            }
        }
        Random random = new Random(20260828L);
        Generator generator = new Generator(random);
        for (int i = 0; i < RANDOM_EXPRESSIONS; i++) {
            cases.add(new Case(generator.expression(), generator.expectedType()));
        }
        compileAndCompare(cases);
    }

    /**
     * Compiles a batch and compares it, bisecting when it does not compile so that one expression the
     * compiler rejects does not lose the rest of the batch.
     */
    private void compileAndCompare(List<Case> batch) throws Exception {
        if (batch.isEmpty()) {
            return;
        }
        Compilation compilation = compile(batch);
        if (compilation.loader() != null) {
            compare(batch, compilation.loader());
            return;
        }
        if (batch.size() == 1) {
            // an expression the compiler rejects is not a failure by itself, the compiler type checks what
            // the interpreter only discovers at evaluation time
            return;
        }
        int half = batch.size() / 2;
        compileAndCompare(new ArrayList<>(batch.subList(0, half)));
        compileAndCompare(new ArrayList<>(batch.subList(half, batch.size())));
    }

    private void compare(List<Case> batch, ClassLoader loader) throws Exception {
        ExpressionFactory compiled = compiledFactory(loader);
        for (Case one : batch) {
            agree(one, "value", value(compiled, loader, one), value(interpreted, loader, one));
            agree(one, "type", operation(compiled, loader, one, "getType"),
                operation(interpreted, loader, one, "getType"));
            agree(one, "read only flag", operation(compiled, loader, one, "isReadOnly"),
                operation(interpreted, loader, one, "isReadOnly"));
            agree(one, "value reference", operation(compiled, loader, one, "getValueReference"),
                operation(interpreted, loader, one, "getValueReference"));
            agree(one, "value after a write", written(compiled, loader, one), written(interpreted, loader, one));
        }
    }

    private static void agree(Case one, String what, Object compiled, Object interpretedValue) {
        if (compiled instanceof Failure && interpretedValue instanceof Failure) {
            return;
        }
        // the one difference between the two paths that is not a defect: the compiler selects an overload
        // from the static types of the arguments, where the interpreter only has their runtime types and
        // may find the reference ambiguous, as both reference implementations do
        if (!(compiled instanceof Failure) && interpretedValue instanceof Failure failure
            && failure.kind().equals("ambiguous")) {
            return;
        }
        if (!Objects.equals(compiled, interpretedValue)) {
            fail("the compiled and the interpreted expression disagree on the " + what + " of " + one
                + ": compiled=" + compiled + " interpreted=" + interpretedValue);
        }
    }

    private Object value(ExpressionFactory factory, ClassLoader loader, Case one) {
        try {
            ELContext context = context(loader);
            return normalize(factory.createValueExpression(context, one.expression(), one.type())
                .getValue(context));
        } catch (Throwable t) {
            return failure(t);
        }
    }

    private Object written(ExpressionFactory factory, ClassLoader loader, Case one) {
        try {
            ELContext context = context(loader);
            ValueExpression expression =
                factory.createValueExpression(context, one.expression(), one.type());
            expression.setValue(context, "written");
            return normalize(expression.getValue(context));
        } catch (Throwable t) {
            return failure(t);
        }
    }

    private Object operation(ExpressionFactory factory, ClassLoader loader, Case one, String operation) {
        try {
            ELContext context = context(loader);
            ValueExpression expression =
                factory.createValueExpression(context, one.expression(), one.type());
            return switch (operation) {
                case "getType" -> String.valueOf(expression.getType(context));
                case "isReadOnly" -> String.valueOf(expression.isReadOnly(context));
                default -> {
                    ValueReference reference = expression.getValueReference(context);
                    yield reference == null ? "null"
                        : reference.getBase().getClass().getName() + "#" + reference.getProperty();
                }
            };
        } catch (Throwable t) {
            return failure(t);
        }
    }

    /**
     * Only whether the operation failed is compared: the two paths reach the same failure by different
     * routes, and the message of one is not the message of the other.
     */
    private static Failure failure(Throwable t) {
        if (t instanceof MethodNotFoundException && String.valueOf(t.getMessage()).contains("ambiguous")) {
            return new Failure("ambiguous");
        }
        return new Failure(t instanceof ELException ? "ELException" : t.getClass().getSimpleName());
    }

    private static Object normalize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Object[] array) {
            return Arrays.toString(array);
        }
        String name = value.getClass().getName();
        if (name.contains(".stream.") || value instanceof Optional<?> || name.endsWith("ELOptional")
            || name.contains("LambdaExpression")) {
            return "<opaque>";
        }
        // an identity hash in a default toString is not a difference between the two paths
        return String.valueOf(value).replaceAll("@[0-9a-f]+", "@x");
    }

    private ELContext context(ClassLoader loader) throws Exception {
        Object book = loader.loadClass(PACKAGE + ".Book").getDeclaredConstructor().newInstance();
        CompiledELContext context = new CompiledELContext().setBean("book", book);
        // the sandbox is a policy of the interpreted path, not a semantic difference, and this compares
        // the semantics
        context.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);
        return context;
    }

    /**
     * The generated source, loaded by its name: the service marker the processor writes is a directory of
     * META-INF/micronaut, which a class loader over the output of the compiler cannot enumerate.
     */
    private static ExpressionFactory compiledFactory(ClassLoader loader) throws Exception {
        Object source = loader.loadClass(PACKAGE + ".Expressions$ELExpressions")
            .getDeclaredConstructor().newInstance();
        return new CompiledExpressionFactory(List.of((ELExpressionSource) source));
    }

    private static Compilation compile(List<Case> cases) {
        try (JavaParser parser = new JavaParser()) {
            List<JavaFileObject> files = new ArrayList<>();
            for (JavaFileObject file : parser.generate(
                source(PACKAGE + ".Book", bookSource()),
                source(PACKAGE + ".Expressions", expressionsSource(cases)))) {
                files.add(file);
            }
            boolean failed = parser.getDiagnosticCollector().getDiagnostics().stream()
                .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
            if (failed || files.isEmpty()) {
                return new Compilation(null);
            }
            return new Compilation(new JavaFileObjectClassLoader(files));
        } catch (Throwable t) {
            return new Compilation(null);
        }
    }

    private static JavaFileObject source(String name, String text) {
        return new SimpleJavaFileObject(URI.create("string:///" + name.replace('.', '/') + ".java"),
            JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return text;
            }
        };
    }

    private static String expressionsSource(List<Case> cases) {
        StringBuilder declarations = new StringBuilder();
        for (int i = 0; i < cases.size(); i++) {
            declarations.append("@ELExpression(value = \"")
                .append(cases.get(i).expression().replace("\\", "\\\\").replace("\"", "\\\""))
                .append("\", expectedType = ").append(cases.get(i).expectedType())
                .append(".class, name = \"e").append(i).append("\")\n");
        }
        return """
            package el.compiled;

            import io.micronaut.el.annotation.*;

            @ELEnvironment(variables = @ELVariable(name = "book", type = Book.class),
                imports = {Integer.class, Long.class, Double.class, Boolean.class, String.class, Math.class})
            %spublic class Expressions {
            }
            """.formatted(declarations);
    }

    private static String bookSource() {
        return """
            package el.compiled;

            import java.util.*;

            public class Book {
                private String title = "EL";
                public String getTitle() { return title; }
                public void setTitle(String t) { title = t; }
                public double getUnitPrice() { return 20.5d; }
                public int getPages() { return 300; }
                public long getIsbn() { return 12345L; }
                public boolean isAvailable() { return true; }
                public List<String> getTags() { return new ArrayList<>(List.of("a", "b", "c")); }
                public List<Long> getSizes() { return new ArrayList<>(List.of(1L, 2L, 3L)); }
                public Map<String, Integer> getStock() { return new LinkedHashMap<>(Map.of("x", 1)); }
                public String[] getCodes() { return new String[]{"p", "q"}; }
                public double discount(double percent) { return getUnitPrice() * (1 - percent / 100); }
                public String repeat(int times) { return getTitle().repeat(times); }
                public boolean matches(String s) { return getTitle().equals(s); }
            }
            """;
    }

    @Test
    void theCompilerResolvesAnOverloadTheInterpreterFindsAmbiguous() throws Exception {
        // Math.max(int, long) has no single best match among max(int,int), max(long,long), max(float,float)
        // and max(double,double) once the arguments are only an Integer and a Long, which is all the
        // interpreter has. Both reference implementations report it ambiguous too. The compiler has the
        // static types and picks max(long, long).
        Case one = new Case("${Math.max(book.pages, 1)}", "Object");
        ClassLoader loader = compile(List.of(one)).loader();
        assertNotNull(loader);
        // the values are normalised to their rendering, which is what the comparison uses
        assertEquals("300", value(compiledFactory(loader), loader, one));
        assertEquals(new Failure("ambiguous"), value(interpreted, loader, one));
    }

    @Test
    void theCompilerAndTheInMemoryCompilationAreWiredUp() {
        assertNotNull(compile(List.of(new Case("${book.title}", "String"))).loader(),
            "the batch of one expression must compile, or every comparison of this test is vacuous");
    }

    private record Compilation(ClassLoader loader) {
    }

    private record Failure(String kind) {
        @Override
        public String toString() {
            return "<" + kind + ">";
        }
    }

    /**
     * An expression and the type it is declared to evaluate to, which the compiled factory matches exactly.
     */
    private record Case(String expression, String expectedType) {

        Class<?> type() {
            return switch (expectedType) {
                case "String" -> String.class;
                case "Boolean" -> Boolean.class;
                default -> Object.class;
            };
        }

        @Override
        public String toString() {
            return "[" + expression + "] as " + expectedType;
        }
    }

    /**
     * Expressions typed against the Book, so that the compiler accepts them.
     */
    private static final class Generator {

        private static final String[] ATOMS = {
            "book.title", "book.unitPrice", "book.pages", "book.isbn", "book.available", "book.tags",
            "book.sizes", "book.stock", "book.codes", "book.discount(10)", "book.repeat(2)",
            "book.matches('EL')", "book.title.length()", "book.stock['x']", "book.codes[0]",
            "book.tags[1]", "Integer.MAX_VALUE", "Math.max(1,2)", "String.valueOf(4)",
            "1", "-7", "2.5", "'a'", "true", "false", "null"};
        private static final String[] STREAMS = {
            "book.tags.stream().toList()", "book.sizes.stream().sum()", "book.sizes.stream().count()",
            "book.tags.stream().filter(t -> t != 'a').toList()", "book.sizes.stream().max()",
            "book.tags.stream().sorted().toList()", "book.sizes.stream().reduce((x,y) -> x + y)",
            "book.tags.stream().findFirst()", "book.sizes.stream().toArray()"};
        private static final String[] BINARY = {
            "+", "-", "*", "/", "%", "+=", "<", ">", "<=", ">=", "==", "!=", "&&", "||", "and", "or",
            "lt", "gt", "eq", "ne"};
        private static final String[] UNARY = {"-", "!", "not ", "empty "};
        private static final String[] EXPECTED_TYPES = {"Object", "Object", "String", "Boolean"};

        private final Random random;

        Generator(Random random) {
            this.random = random;
        }

        String expectedType() {
            return EXPECTED_TYPES[random.nextInt(EXPECTED_TYPES.length)];
        }

        String expression() {
            return switch (random.nextInt(6)) {
                case 0 -> "text ${" + body(3) + "} tail";
                case 1 -> "${" + body(2) + "}${" + body(2) + "}";
                case 2 -> "#{" + body(3) + "}";
                case 3 -> "${book.title}";
                default -> "${" + body(3) + "}";
            };
        }

        private String body(int depth) {
            if (depth <= 0) {
                return pick(ATOMS);
            }
            return switch (random.nextInt(10)) {
                case 0 -> pick(ATOMS);
                case 1 -> "(" + body(depth - 1) + " " + pick(BINARY) + " " + body(depth - 1) + ")";
                case 2 -> pick(UNARY) + "(" + body(depth - 1) + ")";
                case 3 -> "(" + body(depth - 1) + " ? " + body(depth - 1) + " : " + body(depth - 1) + ")";
                case 4 -> "[" + body(depth - 1) + "," + body(depth - 1) + "]";
                case 5 -> "{" + body(depth - 1) + ":" + body(depth - 1) + "}";
                case 6 -> "((x)->" + body(depth - 1) + ")(" + pick(ATOMS) + ")";
                case 7 -> "(" + body(depth - 1) + " ; " + body(depth - 1) + ")";
                case 8 -> pick(STREAMS);
                default -> "(" + body(depth - 1) + ")";
            };
        }

        private String pick(String[] values) {
            return values[random.nextInt(values.length)];
        }
    }
}
