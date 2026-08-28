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
package io.micronaut.el.fuzz;

import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.interpreter.InterpretingELExpressionParser;
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.StandardELContext;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Generates valid, typed expression trees and compares the interpreter with Expressly and Tomcat. The random
 * stream is deliberately based only on {@link Random}, the seed and the case number so every failure can be
 * reproduced.
 */
final class InterpreterDifferentialFuzzTest {

    private static final int MAX_DEPTH = 4;
    private static final String SEED_PROPERTY = "el.fuzz.seed";
    private static final String CASES_PROPERTY = "el.fuzz.cases";

    @Test
    void matchesReferenceImplementations() {
        long seed = Long.parseLong(System.getProperty(SEED_PROPERTY));
        int cases = Integer.parseInt(System.getProperty(CASES_PROPERTY));
        Random random = new Random(seed);
        ExpressionFactory subject = new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser());
        ExpressionFactory expressly = new org.glassfish.expressly.ExpressionFactoryImpl();
        ExpressionFactory tomcat = new org.apache.el.ExpressionFactoryImpl();
        ELContext subjectContext = new StandardELContext(subject);
        ELContext expresslyContext = new StandardELContext(expressly);
        ELContext tomcatContext = new StandardELContext(tomcat);

        for (int caseNumber = 0; caseNumber < cases; caseNumber++) {
            Expr generated = expression(random, MAX_DEPTH);
            String expression = expressionText(random, generated);
            Class<?> expectedType = expectedType(random);
            Outcome actual = evaluate(subject, subjectContext, expression, expectedType);
            Outcome expresslyOutcome = evaluate(expressly, expresslyContext, expression, expectedType);
            Outcome tomcatOutcome = evaluate(tomcat, tomcatContext, expression, expectedType);
            if (equivalent(expresslyOutcome, tomcatOutcome) && !equivalent(actual, expresslyOutcome)) {
                fail("Differential EL fuzz failure (seed=" + seed + ", case=" + caseNumber + ", expression="
                    + expression + ", expectedType=" + expectedType.getTypeName() + ")\nexpected (Expressly): "
                    + expresslyOutcome.describe() + "\nexpected (Tomcat): " + tomcatOutcome.describe()
                    + "\nactual (Micronaut): " + actual.describe());
            }
        }
    }

    private static String expressionText(Random random, Expr expression) {
        return switch (random.nextInt(10)) {
            case 0 -> "prefix ${" + expression.text() + "} suffix";
            case 1 -> "#{ " + expression.text() + " }";
            default -> "${" + expression.text() + "}";
        };
    }

    private static Class<?> expectedType(Random random) {
        return switch (random.nextInt(10)) {
            case 0 -> String.class;
            case 1 -> Boolean.class;
            case 2 -> Long.class;
            case 3 -> Double.class;
            case 4 -> BigDecimal.class;
            default -> Object.class;
        };
    }

    private static Expr expression(Random random, int depth) {
        return switch (random.nextInt(4)) {
            case 0 -> number(random, depth);
            case 1 -> bool(random, depth);
            case 2 -> string(random, depth);
            default -> value(random, depth);
        };
    }

    private static Expr number(Random random, int depth) {
        if (depth == 0 || random.nextInt(4) == 0) {
            return switch (random.nextInt(12)) {
                case 0 -> new Expr("0", Kind.NUMBER);
                case 1 -> new Expr("1", Kind.NUMBER);
                case 2 -> new Expr("-1", Kind.NUMBER);
                case 3 -> new Expr("7", Kind.NUMBER);
                case 4 -> new Expr("2147483648", Kind.NUMBER);
                case 5 -> new Expr("0.0", Kind.NUMBER);
                case 6 -> new Expr("1.5", Kind.NUMBER);
                case 7 -> new Expr("1e3", Kind.NUMBER);
                case 8 -> new Expr("'0'", Kind.NUMBER);
                case 9 -> new Expr("'1'", Kind.NUMBER);
                case 10 -> new Expr("'1.5'", Kind.NUMBER);
                default -> new Expr("null", Kind.NUMBER);
            };
        }
        return switch (random.nextInt(8)) {
            case 0 -> binary(number(random, depth - 1), "+", number(random, depth - 1), Kind.NUMBER);
            case 1 -> binary(number(random, depth - 1), "-", number(random, depth - 1), Kind.NUMBER);
            case 2 -> binary(number(random, depth - 1), "*", number(random, depth - 1), Kind.NUMBER);
            case 3 -> binary(number(random, depth - 1), "div", nonZeroNumber(random), Kind.NUMBER);
            case 4 -> binary(number(random, depth - 1), "mod", nonZeroNumber(random), Kind.NUMBER);
            case 5 -> unary("-", number(random, depth - 1), Kind.NUMBER);
            case 6 -> conditional(random, depth - 1, Kind.NUMBER);
            default -> indexed(random, depth - 1, Kind.NUMBER);
        };
    }

    private static Expr nonZeroNumber(Random random) {
        return switch (random.nextInt(4)) {
            case 0 -> new Expr("1", Kind.NUMBER);
            case 1 -> new Expr("2", Kind.NUMBER);
            case 2 -> new Expr("-3", Kind.NUMBER);
            default -> new Expr("0.5", Kind.NUMBER);
        };
    }

    private static Expr bool(Random random, int depth) {
        if (depth == 0 || random.nextInt(4) == 0) {
            return new Expr(random.nextBoolean() ? "true" : "false", Kind.BOOLEAN);
        }
        return switch (random.nextInt(9)) {
            case 0 -> binary(bool(random, depth - 1), "and", bool(random, depth - 1), Kind.BOOLEAN);
            case 1 -> binary(bool(random, depth - 1), "or", bool(random, depth - 1), Kind.BOOLEAN);
            case 2 -> unary("not ", bool(random, depth - 1), Kind.BOOLEAN);
            case 3 -> binary(number(random, depth - 1), "<", number(random, depth - 1), Kind.BOOLEAN);
            case 4 -> binary(number(random, depth - 1), ">=", number(random, depth - 1), Kind.BOOLEAN);
            case 5 -> equality(random, depth - 1, "==");
            case 6 -> equality(random, depth - 1, "!=");
            case 7 -> empty(random, depth - 1);
            default -> conditional(random, depth - 1, Kind.BOOLEAN);
        };
    }

    private static Expr string(Random random, int depth) {
        if (depth == 0 || random.nextInt(4) == 0) {
            return switch (random.nextInt(6)) {
                case 0 -> new Expr("''", Kind.STRING);
                case 1 -> new Expr("'a'", Kind.STRING);
                case 2 -> new Expr("'0'", Kind.STRING);
                case 3 -> new Expr("'false'", Kind.STRING);
                case 4 -> new Expr("'a b'", Kind.STRING);
                default -> new Expr("'a\\'b'", Kind.STRING);
            };
        }
        return switch (random.nextInt(4)) {
            case 0, 1 -> binary(string(random, depth - 1), "+=", string(random, depth - 1), Kind.STRING);
            case 2 -> conditional(random, depth - 1, Kind.STRING);
            default -> indexed(random, depth - 1, Kind.STRING);
        };
    }

    private static Expr value(Random random, int depth) {
        if (depth == 0) {
            return switch (random.nextInt(4)) {
                case 0 -> number(random, 0);
                case 1 -> bool(random, 0);
                case 2 -> string(random, 0);
                default -> new Expr("null", Kind.ANY);
            };
        }
        return switch (random.nextInt(5)) {
            case 0 -> number(random, depth - 1);
            case 1 -> bool(random, depth - 1);
            case 2 -> string(random, depth - 1);
            case 3 -> indexed(random, depth - 1, randomKind(random));
            default -> conditional(random, depth - 1, randomKind(random));
        };
    }

    private static Expr indexed(Random random, int depth, Kind kind) {
        Expr first = expressionOf(random, depth, kind);
        Expr second = expressionOf(random, depth, kind);
        if (random.nextBoolean()) {
            int index = random.nextInt(2);
            return new Expr("([" + first.text() + "," + second.text() + "][" + index + "])", kind);
        }
        String key = random.nextBoolean() ? "a" : "b";
        return new Expr("({'a':" + first.text() + ",'b':" + second.text() + "}['" + key + "'])", kind);
    }

    private static Expr empty(Random random, int depth) {
        Expr operand = switch (random.nextInt(6)) {
            case 0 -> string(random, depth);
            case 1 -> new Expr("[]", Kind.ANY);
            case 2 -> new Expr("[" + value(random, depth).text() + "]", Kind.ANY);
            case 3 -> new Expr("{}", Kind.ANY);
            case 4 -> new Expr("{'a':" + value(random, depth).text() + "}", Kind.ANY);
            default -> new Expr("null", Kind.ANY);
        };
        return unary("empty ", operand, Kind.BOOLEAN);
    }

    private static Expr conditional(Random random, int depth, Kind kind) {
        Expr condition = bool(random, depth);
        Expr whenTrue = expressionOf(random, depth, kind);
        Expr whenFalse = expressionOf(random, depth, kind);
        return new Expr("(" + condition.text() + "?" + whenTrue.text() + ":" + whenFalse.text() + ")", kind);
    }

    private static Expr equality(Random random, int depth, String operator) {
        Kind kind = Kind.values()[random.nextInt(Kind.values().length - 1)];
        return binary(expressionOf(random, depth, kind), operator, expressionOf(random, depth, kind), Kind.BOOLEAN);
    }

    private static Expr expressionOf(Random random, int depth, Kind kind) {
        return switch (kind) {
            case NUMBER -> number(random, depth);
            case BOOLEAN -> bool(random, depth);
            case STRING -> string(random, depth);
            case ANY -> value(random, depth);
        };
    }

    private static Kind randomKind(Random random) {
        return Kind.values()[random.nextInt(Kind.values().length)];
    }

    private static Expr binary(Expr left, String operator, Expr right, Kind kind) {
        return new Expr("(" + left.text() + " " + operator + " " + right.text() + ")", kind);
    }

    private static Expr unary(String operator, Expr operand, Kind kind) {
        return new Expr("(" + operator + operand.text() + ")", kind);
    }

    private static Outcome evaluate(ExpressionFactory factory,
                                    ELContext context,
                                    String expression,
                                    Class<?> expectedType) {
        try {
            return new Outcome(factory.createValueExpression(context, expression, expectedType).getValue(context), null);
        } catch (RuntimeException e) {
            return new Outcome(null, e);
        }
    }

    private static boolean equivalent(Outcome actual, Outcome expected) {
        if (actual.failure() != null || expected.failure() != null) {
            return actual.failure() != null && expected.failure() != null
                && actual.failure().getClass().equals(expected.failure().getClass());
        }
        return equivalentValue(actual.value(), expected.value());
    }

    private static boolean equivalentValue(@Nullable Object actual, @Nullable Object expected) {
        if (actual == null || expected == null) {
            return actual == expected;
        }
        if (!actual.getClass().equals(expected.getClass())) {
            return false;
        }
        if (actual instanceof BigDecimal actualDecimal && expected instanceof BigDecimal expectedDecimal) {
            return actualDecimal.compareTo(expectedDecimal) == 0;
        }
        if (actual instanceof BigInteger || actual instanceof Number || actual instanceof CharSequence
            || actual instanceof Boolean) {
            return actual.equals(expected);
        }
        if (actual.getClass().isArray()) {
            int length = Array.getLength(actual);
            if (length != Array.getLength(expected)) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (!equivalentValue(Array.get(actual, i), Array.get(expected, i))) {
                    return false;
                }
            }
            return true;
        }
        if (actual instanceof List<?> actualList && expected instanceof List<?> expectedList) {
            if (actualList.size() != expectedList.size()) {
                return false;
            }
            for (int i = 0; i < actualList.size(); i++) {
                if (!equivalentValue(actualList.get(i), expectedList.get(i))) {
                    return false;
                }
            }
            return true;
        }
        if (actual instanceof Map<?, ?> actualMap && expected instanceof Map<?, ?> expectedMap) {
            return actualMap.size() == expectedMap.size() && actualMap.entrySet().stream().allMatch(entry ->
                expectedMap.containsKey(entry.getKey())
                    && equivalentValue(entry.getValue(), expectedMap.get(entry.getKey())));
        }
        return Objects.equals(actual, expected);
    }

    private enum Kind {
        NUMBER,
        BOOLEAN,
        STRING,
        ANY
    }

    private record Expr(String text, Kind kind) {
    }

    private record Outcome(@Nullable Object value, @Nullable RuntimeException failure) {
        String describe() {
            if (failure != null) {
                return "failure " + failure.getClass().getName() + ": " + failure.getMessage();
            }
            return value == null ? "null" : value.getClass().getName() + " " + value;
        }
    }
}
