package io.micronaut.el.processor;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two compilations running at the same time in one JVM, as a Gradle daemon compiles several modules, each see the
 * functions of their own sources only. Each declares an {@code @ELFunction} with the same prefix and name, so a
 * discovery shared by the JVM, as the static {@code ELFunctionDiscovery.CURRENT} was, lets one compilation reset
 * or replace the functions of the other: the expression then binds to the function of the other compilation,
 * fails to find it, or references a class of another package it cannot access.
 */
class ConcurrentFunctionDiscoveryTest {

    private static final int ROUNDS = 40;

    private static String source(String id) {
        return """
            package io.micronaut.el.test.race.%1$s;

            import io.micronaut.el.annotation.*;

            @ELExpression(value = "${ race:who() }", expectedType = String.class, name = "who")
            public class Expressions {
            }

            class Functions%1$s {
                @ELFunction(prefix = "race", name = "who")
                public static String who() { return "%1$s"; }
            }
            """.formatted(id);
    }

    private static String compileAndEvaluate(String id, CountDownLatch start) throws Exception {
        start.await();
        String className = "io.micronaut.el.test.race." + id + ".Expressions";
        GeneratedExpressions generated = GeneratedExpressions.of(className, source(id));
        ELContext context = new CompiledELContext();
        ValueExpression expression = generated.registry().createValueExpression(context, "${ race:who() }", String.class);
        assertNotNull(expression, "the registry of " + id + " does not declare the expression");
        return (String) expression.getValue(context);
    }

    @Test
    void concurrentCompilationsSeeOnlyTheirOwnFunctions() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        List<String> failures = new ArrayList<>();
        try {
            for (int round = 0; round < ROUNDS; round++) {
                // both compilations are released together, so that their visitors interleave
                CountDownLatch start = new CountDownLatch(1);
                List<Future<String>> results = new ArrayList<>(2);
                for (String id : List.of("a", "b")) {
                    Callable<String> compilation = () -> compileAndEvaluate(id, start);
                    results.add(executor.submit(compilation));
                }
                start.countDown();
                for (int i = 0; i < results.size(); i++) {
                    String id = i == 0 ? "a" : "b";
                    try {
                        String result = results.get(i).get(2, TimeUnit.MINUTES);
                        if (!id.equals(result)) {
                            failures.add("round " + round + ": the compilation " + id + " called the function of " + result);
                        }
                    } catch (java.util.concurrent.ExecutionException e) {
                        failures.add("round " + round + ": the compilation " + id + " failed: " + e.getCause());
                    }
                }
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.MINUTES));
        }
        assertEquals(List.of(), failures, failures.size() + " of " + (2 * ROUNDS) + " compilations saw the other one's function");
    }
}
