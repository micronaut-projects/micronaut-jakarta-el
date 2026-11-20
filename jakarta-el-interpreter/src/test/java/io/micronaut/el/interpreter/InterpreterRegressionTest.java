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
package io.micronaut.el.interpreter;

import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.parser.ELParserOptions;
import io.micronaut.el.parser.ELParsingException;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.LambdaExpression;
import jakarta.el.StandardELContext;
import jakarta.el.ValueExpression;
import jakarta.el.ValueReference;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regressions of the interpreter. The expressions that can be generated have compile-time counterparts with the
 * same text and expected result in {@code test-suite-java}, {@code InterpreterRegressionExpressionsTest}; the
 * contracts of the runtime API alone - the parser options, {@code getValueReference}, an escaping lambda invoked
 * with another context, the lock of the cache - are documented below where they have none.
 */
class InterpreterRegressionTest {

    private static final int ELEMENTS = 200_000;

    private final ExpressionFactory factory = new CompiledExpressionFactory(List.of(),
        new InterpretingELExpressionParser());

    // --- 1. the parser limits ---

    /**
     * The long expressions of {@code InterpreterRegressionExpressions}, evaluated by the interpreter: each one is
     * accepted by Expressly and Tomcat, and was rejected by the original limits.
     */
    @Test
    void longOrdinaryExpressionsAreInterpreted() {
        ELContext context = context();
        assertEquals("x".repeat(17_000) + "1", value(context, longText()));
        assertEquals(600, ((List<?>) value(context, longList())).size());
        assertEquals("1 ".repeat(300), value(context, manyParts()));
        assertEquals(150L, value(context, longSum()));
        assertEquals(true, value(context, longConjunction()));
        assertEquals("a".repeat(120), value(context, longConcatenation()));
        assertEquals("a", value(context, longTrimChain()));
    }

    /**
     * The limits are an API of the runtime factory, so no generated expression can exercise them: the processor
     * parses an {@code @ELExpression} with the default limits, which the long expressions of
     * {@code InterpreterRegressionExpressions} show compile.
     */
    @Test
    void theLimitsAreConfigurableThroughTheFactory() throws Exception {
        String chain = "${" + "1+".repeat(2_999) + "1}";
        ELContext context = context();
        assertThrows(ELParsingException.class, () -> factory.createValueExpression(context, chain, Object.class));

        // a taller tree needs a larger stack, as the options document
        ExpressionFactory raised = new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser(
            ELParserOptions.defaults().withMaxTreeHeight(4_000)));
        assertEquals(3_000L, onStack(16 * 1024 * 1024,
            () -> raised.createValueExpression(context, chain, Object.class).getValue(context)));

        ExpressionFactory lowered = new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser(
            ELParserOptions.defaults().withMaxLength(20).withMaxTokens(8).withMaxDepth(2)));
        assertThrows(ELParsingException.class,
            () -> lowered.createValueExpression(context, "${'" + "x".repeat(30) + "'}", Object.class));
        assertThrows(ELParsingException.class,
            () -> lowered.createValueExpression(context, "${1+1+1+1+1}", Object.class));
        assertThrows(ELParsingException.class,
            () -> lowered.createValueExpression(context, "${((1))}", Object.class));
        assertEquals((Object) 2L, lowered.createValueExpression(context, "${(1+1)}", Object.class).getValue(context));
        assertThrows(ELParsingException.class,
            () -> lowered.createMethodExpression(context, "${bean.join(((1)))}", Object.class, null));
    }

    @Test
    void theLimitsAreReadFromSystemPropertiesByTheServiceLoadedFactory() {
        String expression = "${'" + "x".repeat(40) + "'}";
        System.setProperty(ELParserOptions.MAX_LENGTH_PROPERTY, "30");
        try {
            // ExpressionFactory.newInstance() loads the interpreter as a service, through its default constructor
            ExpressionFactory loaded = ExpressionFactory.newInstance();
            ELContext context = new StandardELContext(loaded);
            assertThrows(ELParsingException.class, () -> loaded.createValueExpression(context, expression, Object.class));
            assertEquals(30, new InterpretingELExpressionParser().getOptions().maxLength());
        } finally {
            System.clearProperty(ELParserOptions.MAX_LENGTH_PROPERTY);
        }
        ExpressionFactory loaded = ExpressionFactory.newInstance();
        ELContext context = new StandardELContext(loaded);
        assertEquals("x".repeat(40), loaded.createValueExpression(context, expression, Object.class).getValue(context));
    }

    /**
     * Deep real nesting is still rejected, and a tree too tall is rejected before any recursive walk of it, so
     * neither fails with a {@link StackOverflowError}; a tree at the limit evaluates on a thread with half the
     * usual stack.
     */
    @Test
    void pathologicalInputIsStillBoundedWithoutOverflowingTheStack() throws Exception {
        ELContext context = context();
        assertThrows(ELParsingException.class, () -> factory.createValueExpression(context,
            "${" + "(".repeat(5_000) + "1" + ")".repeat(5_000) + "}", Object.class));
        assertThrows(ELParsingException.class, () -> factory.createValueExpression(context,
            "${" + "1+".repeat(50_000) + "1}", Object.class));
        assertThrows(ELParsingException.class, () -> factory.createValueExpression(context,
            "${" + "-".repeat(5_000) + "1}", Object.class));

        // the tree of the expression is three nodes higher than the chain: the eval, the property and its name
        int terms = ELParserOptions.DEFAULT_MAX_TREE_HEIGHT - 3;
        assertEquals("n" + "a".repeat(terms), onStack(512 * 1024, () -> {
            ELContext own = context();
            return new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser())
                .createValueExpression(own, "${bean.name" + " += 'a'".repeat(terms) + "}", Object.class)
                .getValue(own);
        }));
    }

    // --- 2. a default method of a platform interface ---

    @Test
    void aDefaultMethodOfAPlatformInterfaceIsInvokedOnTheProxy() {
        ELContext context = context();
        assertEquals("11", value(context, "${bean.iu(x -> x * 10)}"));
        assertEquals("21", value(context, "${bean.op(x -> x * 10)}"));
    }

    // --- 3. lambdas invoked concurrently ---

    @Test
    void aLambdaPresentedAsAnInterfaceIsInvokedConcurrently() {
        ELContext context = context();
        List<?> mapped = (List<?>) value(context, "${bean.parallelMap(x -> x + 0)}");
        assertEquals(ELEMENTS, mapped.size());
        for (int i = 0; i < ELEMENTS; i++) {
            assertEquals((long) i, mapped.get(i));
        }
        List<?> nested = (List<?>) value(context, "${(k -> bean.parallelMap(x -> x + k))(5)}");
        assertEquals(ELEMENTS, nested.size());
        assertEquals(5L, nested.get(0));
        assertEquals((long) ELEMENTS + 4, nested.get(ELEMENTS - 1));
    }

    /**
     * A lambda held by a variable is shared by every evaluation reading it; evaluating it concurrently with a
     * context of each own does not move the context of the lambda.
     */
    @Test
    void aSharedLambdaIsInvokedConcurrentlyWithTheContextOfEachEvaluation() throws Exception {
        ELContext creating = context();
        LambdaExpression shared = (LambdaExpression) value(creating, "${x -> x + 1}");
        ValueExpression call = factory.createValueExpression(null, "${f(i)}", Object.class);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < 2_000; i++) {
                long argument = i;
                results.add(executor.submit(() -> {
                    ELContext own = new StandardELContext(factory);
                    own.getVariableMapper().setVariable("f", factory.createValueExpression(shared, Object.class));
                    own.getVariableMapper().setVariable("i", factory.createValueExpression(argument, Object.class));
                    return call.getValue(own);
                }));
            }
            for (int i = 0; i < results.size(); i++) {
                assertEquals(i + 1L, results.get(i).get(1, TimeUnit.MINUTES));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    // --- 4. getValueReference of a variable ---

    /**
     * {@code getValueReference} is an API of a created expression, with no generated counterpart: a variable is
     * resolved where the expression is created, and a compiled expression declares its variables instead.
     */
    @Test
    void theValueReferenceOfAVariableIsTheOneOfItsExpression() {
        ELContext context = context();
        assertNull(factory.createValueExpression(context, "${bean}", Object.class).getValueReference(context));
        context.getVariableMapper().setVariable("alias",
            factory.createValueExpression(context, "${bean.name}", Object.class));
        ValueReference reference = factory.createValueExpression(context, "${alias}", Object.class)
            .getValueReference(context);
        assertInstanceOf(Bean.class, reference.getBase());
        assertEquals("name", reference.getProperty());
        ValueReference property = factory.createValueExpression(context, "${bean.name}", Object.class)
            .getValueReference(context);
        assertEquals("name", property.getProperty());
    }

    // --- 5. a null method name ---

    @Test
    void aMethodNamedNullIsRejected() {
        ELContext context = context();
        assertThrows(ELException.class, () -> value(context, "${bean[null]()}"));
        assertThrows(ELException.class, () -> value(context, "${bean[null](1)}"));
        assertNull(value(context, "${bean[null]}"));
    }

    // --- 6. the result of a method expression ---

    @Test
    void theResultOfAMethodExpressionIsNotCoercedToTheExpectedReturnType() {
        ELContext context = context();
        assertEquals("n", factory.createMethodExpression(context, "${bean.getName}", Integer.class,
            new Class<?>[0]).invoke(context, new Object[0]));
        assertEquals(2, factory.createMethodExpression(context, "${bean.two}", String.class,
            new Class<?>[0]).invoke(context, new Object[0]));
        assertEquals("n", factory.createMethodExpression(context, "${bean.getName()}", Integer.class,
            null).invoke(context, null));
        // a literal-expression is still coerced, by the section 1.2.1.2
        assertEquals(5, factory.createMethodExpression(context, "5", Integer.class,
            new Class<?>[0]).invoke(context, new Object[0]));
    }

    // --- 7. an escaping lambda keeps its variables ---

    /**
     * A lambda returned to Java and invoked with another context is a contract of the runtime API. Its generated
     * counterpart, {@code ${y -> s += y}} declared with an {@code @ELVariable s}, does not hold yet: the compiled
     * lambda of the core runtime resolves {@code s} against the invoking context and throws
     * {@code PropertyNotFoundException}, which is outside the interpreter. The differential test of the fuzz module
     * checks the interpreter against both reference implementations instead.
     */
    @Test
    void anEscapingLambdaKeepsTheVariablesOfTheExpressionCreatingIt() {
        ELContext context = context();
        context.getVariableMapper().setVariable("s", factory.createValueExpression("str", String.class));
        ValueExpression expression = factory.createValueExpression(context, "${y -> s += y}", Object.class);
        LambdaExpression lambda = (LambdaExpression) expression.getValue(context);

        assertEquals("str2", lambda.invoke(new StandardELContext(factory), 2));
        context.getVariableMapper().setVariable("s", factory.createValueExpression("changed", String.class));
        assertEquals("str2", lambda.invoke(context, 2));
        assertEquals("str2", ((LambdaExpression) expression.getValue(new StandardELContext(factory)))
            .invoke(new StandardELContext(factory), 2));
        // a lambda parameter shadows a variable of the same name, which is therefore not captured
        LambdaExpression shadowing = (LambdaExpression) factory.createValueExpression(context, "${s -> s += 1}",
            Object.class).getValue(context);
        assertEquals("p1", shadowing.invoke(new StandardELContext(factory), "p"));
    }

    // --- 8. no lock around the parse ---

    /**
     * The cache of the parsed strings is an implementation detail with no generated counterpart: a thread holding
     * the monitor of the parser, which the parse used to synchronize on, does not hold up another parse.
     */
    @Test
    void creatingAnExpressionTakesNoGlobalLock() throws Exception {
        InterpretingELExpressionParser parser = new InterpretingELExpressionParser();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            synchronized (parser) {
                Future<Object> created = executor.submit(
                    () -> parser.createValueExpression(null, "${1 + 41}", Object.class).getValue(context()));
                assertEquals(42L, created.get(30, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void theCacheStaysCorrectUnderConcurrency() throws Exception {
        InterpretingELExpressionParser parser = new InterpretingELExpressionParser();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<Object>> results = new ArrayList<>();
            for (int i = 0; i < 20_000; i++) {
                int value = i % (InterpretingELExpressionParser.CACHE_SIZE * 2);
                results.add(executor.submit(
                    () -> parser.createValueExpression(null, "${" + value + " + 1}", Object.class).getValue(context())));
            }
            for (int i = 0; i < results.size(); i++) {
                assertEquals((long) (i % (InterpretingELExpressionParser.CACHE_SIZE * 2)) + 1,
                    results.get(i).get(1, TimeUnit.MINUTES));
            }
        } finally {
            executor.shutdownNow();
        }
        assertTrue(parser.cachedExpressions() <= InterpretingELExpressionParser.CACHE_SIZE);

        // a string that does not parse is not cached
        assertThrows(ELParsingException.class, () -> parser.createValueExpression(null, "${1 +}", Object.class));
        assertFalse(parser.isCached("${1 +}"));
        // the same string shares its parse
        ValueExpression first = parser.createValueExpression(null, "${7 * 6}", Object.class);
        ValueExpression second = parser.createValueExpression(null, "${7 * 6}", Object.class);
        assertEquals(first, second);
        assertTrue(parser.isCached("${7 * 6}"));
    }

    // --- the shared fixture ---

    static String longText() {
        return "x".repeat(17_000) + "${1}";
    }

    static String longList() {
        return "${[" + "1,".repeat(599) + "1]}";
    }

    static String manyParts() {
        return "${1} ".repeat(300);
    }

    static String longSum() {
        return "${" + "1+".repeat(149) + "1}";
    }

    static String longConjunction() {
        return "${" + "true and ".repeat(109) + "true}";
    }

    static String longConcatenation() {
        return "${" + "'a' += ".repeat(119) + "'a'}";
    }

    static String longTrimChain() {
        return "${' a '" + ".trim()".repeat(120) + "}";
    }

    private static Object onStack(long stackSize, java.util.concurrent.Callable<Object> action) throws Exception {
        AtomicReference<Object> result = new AtomicReference<>();
        Thread thread = new Thread(null, () -> {
            try {
                result.set(action.call());
            } catch (Throwable t) {
                result.set(t);
            }
        }, "sized-stack", stackSize);
        thread.start();
        thread.join(TimeUnit.MINUTES.toMillis(1));
        if (result.get() instanceof Throwable failure) {
            throw new AssertionError(failure);
        }
        return result.get();
    }

    private Object value(ELContext context, String expression) {
        return factory.createValueExpression(context, expression, Object.class).getValue(context);
    }

    private ELContext context() {
        StandardELContext context = new StandardELContext(factory);
        context.getVariableMapper().setVariable("bean", factory.createValueExpression(new Bean(), Bean.class));
        return context;
    }

    /**
     * The bean of the regressions, its compile-time twin is {@code InterpreterRegressionBean}.
     */
    public static final class Bean {

        public String getName() {
            return "n";
        }

        public int two() {
            return 2;
        }

        public String join(Object value) {
            return String.valueOf(value);
        }

        public String iu(IntUnaryOperator f) {
            return "" + f.andThen(x -> x + 1).applyAsInt(1);
        }

        public String op(Operator f) {
            return "" + f.twice(1);
        }

        public List<Object> parallelMap(Mapper mapper) {
            return IntStream.range(0, ELEMENTS).parallel().mapToObj(mapper::map).toList();
        }
    }

    /**
     * An interface of the application with a default method.
     */
    public interface Operator {

        int apply(int value);

        default int twice(int value) {
            return apply(value) + apply(value) + 1;
        }
    }

    /**
     * An interface of the application, which the reflective proxy implements.
     */
    public interface Mapper {

        Object map(int value);
    }
}
