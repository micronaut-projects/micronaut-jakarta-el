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
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.LambdaExpression;
import jakarta.el.StandardELContext;
import jakarta.el.ValueReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The interpreter regressions found by comparison with Expressly and Tomcat, run against all three: each case
 * asserts the answer of the references and of the interpreter alike.
 */
final class InterpreterRegressionDifferentialTest {

    private static final List<ExpressionFactory> FACTORIES = List.of(
        new org.glassfish.expressly.ExpressionFactoryImpl(),
        new org.apache.el.ExpressionFactoryImpl(),
        new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser()));

    @Test
    void longOrdinaryExpressionsAreAccepted() {
        for (ExpressionFactory factory : FACTORIES) {
            ELContext context = context(factory);
            String name = factory.getClass().getName();
            assertEquals("x".repeat(17_000) + "1", value(factory, context, "x".repeat(17_000) + "${1}"), name);
            assertEquals(600, ((List<?>) value(factory, context, "${[" + "1,".repeat(599) + "1]}")).size(), name);
            assertEquals("1 ".repeat(300), value(factory, context, "${1} ".repeat(300)), name);
            assertEquals(150L, value(factory, context, "${" + "1+".repeat(149) + "1}"), name);
            assertEquals(true, value(factory, context, "${" + "true and ".repeat(109) + "true}"), name);
            assertEquals("a".repeat(120), value(factory, context, "${" + "'a' += ".repeat(119) + "'a'}"), name);
            assertEquals("a", value(factory, context, "${' a '" + ".trim()".repeat(120) + "}"), name);
        }
    }

    @Test
    void theValueReferenceOfAVariableIsTheOneOfItsExpression() {
        for (ExpressionFactory factory : FACTORIES) {
            ELContext context = context(factory);
            String name = factory.getClass().getName();
            assertNull(factory.createValueExpression(context, "${bean}", Object.class).getValueReference(context), name);
            context.getVariableMapper().setVariable("alias",
                factory.createValueExpression(context, "${bean.name}", Object.class));
            ValueReference reference = factory.createValueExpression(context, "${alias}", Object.class)
                .getValueReference(context);
            assertInstanceOf(Bean.class, reference.getBase(), name);
            assertEquals("name", reference.getProperty(), name);
        }
    }

    @Test
    void aMethodNamedNullIsRejected() {
        for (ExpressionFactory factory : FACTORIES) {
            ELContext context = context(factory);
            assertThrows(ELException.class, () -> value(factory, context, "${bean[null]()}"),
                factory.getClass().getName());
            assertNull(value(factory, context, "${bean[null]}"), factory.getClass().getName());
        }
    }

    @Test
    void theResultOfAMethodExpressionIsNotCoerced() {
        for (ExpressionFactory factory : FACTORIES) {
            ELContext context = context(factory);
            String name = factory.getClass().getName();
            assertEquals("n", factory.createMethodExpression(context, "${bean.getName}", Integer.class,
                new Class<?>[0]).invoke(context, new Object[0]), name);
            assertEquals(2, factory.createMethodExpression(context, "${bean.two}", String.class,
                new Class<?>[0]).invoke(context, new Object[0]), name);
            assertEquals(2, factory.createMethodExpression(context, "${bean.two()}", void.class,
                null).invoke(context, null), name);
            assertEquals(5, factory.createMethodExpression(context, "5", Integer.class,
                new Class<?>[0]).invoke(context, new Object[0]), name);
        }
    }

    @Test
    void anEscapingLambdaKeepsTheVariablesOfTheExpressionCreatingIt() {
        for (ExpressionFactory factory : FACTORIES) {
            String name = factory.getClass().getName();
            Function<ELContext, LambdaExpression> create = context -> (LambdaExpression) factory
                .createValueExpression(context, "${y -> s += y}", Object.class).getValue(context);

            ELContext context = context(factory);
            context.getVariableMapper().setVariable("s", factory.createValueExpression("str", String.class));
            LambdaExpression lambda = create.apply(context);
            assertEquals("str2", lambda.invoke(new StandardELContext(factory), 2), name);
            context.getVariableMapper().setVariable("s", factory.createValueExpression("changed", String.class));
            assertEquals("str2", lambda.invoke(context, 2), name);
        }
    }

    private static Object value(ExpressionFactory factory, ELContext context, String expression) {
        return factory.createValueExpression(context, expression, Object.class).getValue(context);
    }

    private static ELContext context(ExpressionFactory factory) {
        StandardELContext context = new StandardELContext(factory);
        context.getVariableMapper().setVariable("bean", factory.createValueExpression(new Bean(), Object.class));
        return Objects.requireNonNull(context);
    }

    /**
     * A plain public bean, reached the same way by every implementation.
     */
    public static final class Bean {

        public String getName() {
            return "n";
        }

        public int two() {
            return 2;
        }
    }
}
