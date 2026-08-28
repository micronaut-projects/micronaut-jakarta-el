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

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.parser.ELNodes;
import io.micronaut.el.parser.ELParser;
import io.micronaut.el.parser.ELParsingException;
import io.micronaut.el.parser.ast.ELNode;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Generates expressions and asserts the invariants that hold for every one of them, valid or not.
 *
 * <p>The generator is seeded, so a run reproduces exactly; the seeds it starts from are the constructs of the
 * language, which it then combines and mutates. A failure is reduced to the shortest expression that still
 * fails before it is reported.</p>
 *
 * <p>Raise {@code micronaut.el.fuzz.iterations} to run a longer campaign than the build does, and
 * {@code micronaut.el.fuzz.seed} to start it elsewhere.</p>
 */
class ELFuzzTest {

    private static final int ITERATIONS =
        Integer.getInteger("micronaut.el.fuzz.iterations", 20_000);
    private static final long SEED =
        Long.getLong("micronaut.el.fuzz.seed", 20260828L);

    private final ExpressionFactory factory = new CompiledExpressionFactory();

    @Test
    void theParserOnlyEverFailsWithAParsingException() {
        fuzz(expression -> {
            try {
                ELParser.parse(expression);
            } catch (ELParsingException e) {
                // the only failure the parser is allowed
            }
            return true;
        }, "the parser threw something other than an ELParsingException");
    }

    @Test
    void theCanonicalFormOfAnExpressionReParsesToItself() {
        // jakarta.el.Expression.equals compares expressions by their canonical form, so a form that does not
        // parse back, or that renders differently the second time, breaks the equality of the specification
        fuzz(expression -> {
            ELNode node;
            try {
                node = ELParser.parse(expression);
            } catch (ELParsingException e) {
                return true;
            }
            String canonical = ELNodes.canonical(node);
            return canonical.equals(ELNodes.canonical(ELParser.parse(canonical)));
        }, "the canonical form of the expression does not re-parse to itself");
    }

    @Test
    void theEvaluationOnlyEverFailsWithAnExpressionLanguageException() {
        // jakarta.el.ValueExpression.getValue is specified to fail with an ELException, so an exception of
        // the runtime reaching the caller is a failure of this implementation, not of the expression
        fuzz(expression -> {
            ELContext context = context();
            try {
                factory.createValueExpression(context, expression, Object.class).getValue(context);
            } catch (ELException e) {
                // the failure an expression is allowed
            } catch (IllegalArgumentException e) {
                // jakarta.el.ListELResolver and jakarta.el.ArrayELResolver of the specification raise this,
                // a NumberFormatException included, for an index that is not an integer
            } catch (ArithmeticException e) {
                // an integer division or modulo by zero, which the section 1.7.3 calls an error but which
                // every implementation of the specification lets through as it is
            }
            return true;
        }, "the evaluation threw something other than an ELException");
    }

    private void fuzz(Predicate<String> invariant, String message) {
        Random random = new Random(SEED);
        Generator generator = new Generator(random);
        List<String> corpus = new ArrayList<>(Generator.SEEDS);
        for (int i = 0; i < ITERATIONS; i++) {
            String expression = switch (random.nextInt(10)) {
                case 0, 1, 2, 3, 4, 5 -> generator.expression();
                case 6, 7, 8 -> generator.mutate(corpus.get(random.nextInt(corpus.size())));
                default -> generator.garbage();
            };
            if (!holds(invariant, expression)) {
                fail(message + ": [" + minimize(invariant, expression) + "]"
                    + " (seed " + SEED + ", iteration " + i + ", from [" + expression + "])");
            }
            if (corpus.size() < 2000 && random.nextInt(20) == 0) {
                corpus.add(expression);
            }
        }
    }

    private static boolean holds(Predicate<String> invariant, String expression) {
        try {
            return invariant.test(expression);
        } catch (StackOverflowError | Exception e) {
            return false;
        }
    }

    /**
     * The shortest prefix, suffix and single character deletion of the expression that still breaks the
     * invariant, which is enough to make a generated expression readable.
     */
    private static String minimize(Predicate<String> invariant, String expression) {
        String shortest = expression;
        boolean reduced = true;
        while (reduced && shortest.length() > 1) {
            reduced = false;
            for (int i = 0; i < shortest.length(); i++) {
                String candidate = shortest.substring(0, i) + shortest.substring(i + 1);
                if (!holds(invariant, candidate)) {
                    shortest = candidate;
                    reduced = true;
                    break;
                }
            }
        }
        return shortest;
    }

    private CompiledELContext context() {
        return new CompiledELContext()
            .setBean("a", 1L)
            .setBean("b", 2.5d)
            .setBean("s", "text")
            .setBean("l", new ArrayList<>(Arrays.asList(1L, null, 3L)))
            .setBean("m", new LinkedHashMap<>(Map.of("k", "v")));
    }

    /**
     * A generator of expressions of the grammar of the section 1.26 of the specification, and of the
     * mutations and the random strings that the grammar does not cover.
     */
    private static final class Generator {

        static final List<String> SEEDS = List.of(
            "${1+1}", "${'a'+='b'}", "${[1,2,3].stream().sum()}", "${{1:2}}", "${(x)->x}",
            "${a.b.c}", "${a[0]}", "${empty a}", "${1 gt 2}", "${null}", "text${1}text",
            "${Integer.MAX_VALUE}", "${x=1;x}", "${(x->x)(1)}", "${[1,2].stream().map(y->y*2).toList()}",
            "#{1+1}", "${'\\''}", "${1e10}", "${.5}", "${true?1:2}", "\\${a}", "${{1,2}}", "${a:b()}");

        private static final String[] VARIABLES = {"a", "b", "s", "l", "m", "undefined"};
        private static final String[] IMPORTED = {"Integer", "Long", "Double", "Boolean", "String", "Math"};
        private static final String[] BINARY = {
            "+", "-", "*", "/", "div", "%", "mod", "+=", "<", ">", "<=", ">=", "lt", "gt", "le", "ge",
            "==", "!=", "eq", "ne", "&&", "and", "||", "or"};
        private static final String[] UNARY = {"-", "!", "not ", "empty "};
        private static final String[] OPERATIONS = {
            ".stream().toList()", ".stream().filter(x->x!=null).toList()", ".stream().map(x->x).toList()",
            ".stream().count()", ".stream().sum()", ".stream().distinct().toList()",
            ".stream().limit(2).toList()", ".stream().substream(1).toList()", ".stream().sorted().toList()",
            ".stream().average()", ".stream().max()", ".stream().min()", ".stream().anyMatch(x->true)",
            ".stream().findFirst()", ".stream().reduce((x,y)->x)", ".stream().flatMap(x->[x]).toList()",
            ".stream().peek(x->x).toList()", ".stream().toArray()", ".stream().iterator()",
            ".stream().forEach(x->x)", ".stream().sorted((x,y)->0).toList()", ".size()", ".isEmpty()",
            ".toString()", ".hashCode()"};
        private static final String[] STATIC_MEMBERS = {
            "MAX_VALUE", "MIN_VALUE", "TRUE", "PI", "parseInt('1')", "valueOf('1')", "valueOf(1)",
            "abs(-1)", "max(1,2)", "compare(1,2)"};
        private static final char[] ALPHABET = "${}#[]().,;:?'\"+-*/%<>=!&|abcXY01 \\_\t".toCharArray();

        private final Random random;

        Generator(Random random) {
            this.random = random;
        }

        String expression() {
            return switch (random.nextInt(10)) {
                case 0 -> text() + "${" + expression(4) + "}" + text();
                case 1 -> "${" + expression(4) + "}${" + expression(4) + "}";
                case 2 -> "#{" + expression(4) + "}";
                default -> "${" + expression(4) + "}";
            };
        }

        String garbage() {
            StringBuilder builder = new StringBuilder();
            int length = random.nextInt(40);
            for (int i = 0; i < length; i++) {
                builder.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            }
            return builder.toString();
        }

        String mutate(String seed) {
            StringBuilder builder = new StringBuilder(seed);
            int mutations = 1 + random.nextInt(3);
            for (int i = 0; i < mutations && !builder.isEmpty(); i++) {
                int position = random.nextInt(builder.length());
                switch (random.nextInt(3)) {
                    case 0 -> builder.deleteCharAt(position);
                    case 1 -> builder.insert(position, ALPHABET[random.nextInt(ALPHABET.length)]);
                    default -> builder.setCharAt(position, ALPHABET[random.nextInt(ALPHABET.length)]);
                }
            }
            return builder.toString();
        }

        private String text() {
            String[] texts = {"", "x", " ", "a b", "\\${", "\\#{", "$", "#", "]", "}", "\\\\"};
            return texts[random.nextInt(texts.length)];
        }

        private String expression(int depth) {
            if (depth <= 0) {
                return atom();
            }
            return switch (random.nextInt(16)) {
                case 0 -> atom();
                case 1 -> "(" + expression(depth - 1) + " " + pick(BINARY) + " " + expression(depth - 1) + ")";
                case 2 -> pick(UNARY) + "(" + expression(depth - 1) + ")";
                case 3 -> "(" + expression(depth - 1) + " ? " + expression(depth - 1)
                    + " : " + expression(depth - 1) + ")";
                case 4 -> "[" + elements(depth) + "]";
                case 5 -> "{" + elements(depth) + "}";
                case 6 -> "{" + entries(depth) + "}";
                case 7 -> "(" + expression(depth - 1) + ")";
                case 8 -> "((x)->" + expression(depth - 1) + ")(" + atom() + ")";
                case 9 -> "(x->" + expression(depth - 1) + ")(" + atom() + ")";
                case 10 -> "[" + elements(depth) + "]" + pick(OPERATIONS);
                case 11 -> atom() + "[" + expression(depth - 1) + "]";
                case 12 -> pick(IMPORTED) + "." + pick(STATIC_MEMBERS);
                case 13 -> "(" + expression(depth - 1) + " ; " + expression(depth - 1) + ")";
                case 14 -> "(" + pick(VARIABLES) + " = " + expression(depth - 1) + ")";
                default -> "(" + expression(depth - 1) + ")" + pick(OPERATIONS);
            };
        }

        private String elements(int depth) {
            StringBuilder builder = new StringBuilder();
            int count = random.nextInt(4);
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(expression(depth - 1));
            }
            return builder.toString();
        }

        private String entries(int depth) {
            StringBuilder builder = new StringBuilder();
            int count = 1 + random.nextInt(3);
            for (int i = 0; i < count; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(expression(depth - 1)).append(':').append(expression(depth - 1));
            }
            return builder.toString();
        }

        private String atom() {
            return switch (random.nextInt(14)) {
                case 0 -> String.valueOf(random.nextInt(1000) - 500);
                case 1 -> random.nextInt(100) + "." + random.nextInt(100);
                case 2 -> "'" + word() + "'";
                case 3 -> "\"" + word() + "\"";
                case 4 -> "true";
                case 5 -> "false";
                case 6 -> "null";
                case 7 -> pick(VARIABLES);
                case 8 -> String.valueOf(random.nextLong());
                case 9 -> random.nextInt(10) + "e" + (random.nextInt(20) - 10);
                case 10 -> "[1,2,3]";
                case 11 -> "{'k':'v'}";
                case 12 -> "{1,2,3}";
                default -> "''";
            };
        }

        private String word() {
            StringBuilder builder = new StringBuilder();
            int length = random.nextInt(4);
            String alphabet = "abcXY 019_-.";
            for (int i = 0; i < length; i++) {
                builder.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            return builder.toString();
        }

        private String pick(String[] values) {
            return values[random.nextInt(values.length)];
        }
    }
}
