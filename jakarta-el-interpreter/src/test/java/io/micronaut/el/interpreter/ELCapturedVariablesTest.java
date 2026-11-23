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
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.EvaluationListener;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The context the body of an escaping lambda expression is evaluated with when the invoking context does not
 * resolve the variables the lambda expression saw where it was created. The expression-level behaviour,
 * {@code ${y -> s += y}} invoked with another context, is covered with its generated twin by
 * {@link InterpreterRegressionTest#anEscapingLambdaKeepsTheVariablesOfTheExpressionCreatingIt}; this checks the
 * Java contract of that context, that it delegates everything else to the invoking one, which has no expression
 * text of its own and so no generated twin.
 */
class ELCapturedVariablesTest {

    private final ExpressionFactory factory =
        new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser());

    private final Function<ELContext, @Nullable Object> body = ELContext::getVariableMapper;

    @Test
    void aBodyIsOnlyWrappedWhenTheCreatingContextResolvesAFreeIdentifier() {
        CompiledELContext creating = new CompiledELContext();
        creating.getVariableMapper().setVariable("s", literal("str"));

        assertSame(body, ELCapturedVariables.capture(creating, new String[0], body));
        assertSame(body, ELCapturedVariables.capture(new NoVariablesContext(), new String[]{"s"}, body));
        assertSame(body, ELCapturedVariables.capture(creating, new String[]{"unbound"}, body));
        assertNotSame(body, ELCapturedVariables.capture(creating, new String[]{"unbound", "s"}, body));
    }

    @Test
    void aContextResolvingTheSameVariablesIsUsedAsItIs() {
        CompiledELContext creating = new CompiledELContext();
        creating.getVariableMapper().setVariable("s", literal("str"));
        AtomicReference<ELContext> seen = new AtomicReference<>();
        Function<ELContext, @Nullable Object> captured = ELCapturedVariables.capture(creating,
            new String[]{"unbound", "s"}, context -> {
                seen.set(context);
                return "evaluated";
            });

        assertEquals("evaluated", captured.apply(creating));

        assertSame(creating, seen.get());
    }

    @Test
    void anotherContextAnswersTheCapturedVariablesAndDelegatesEverythingElse() {
        CompiledELContext creating = new CompiledELContext();
        ValueExpression str = literal("str");
        creating.getVariableMapper().setVariable("s", str);
        CompiledELContext invoking = new CompiledELContext();
        ValueExpression other = literal("other");
        invoking.getVariableMapper().setVariable("o", other);
        invoking.getVariableMapper().setVariable("s", literal("rebound"));
        List<String> events = new ArrayList<>();
        invoking.addEvaluationListener(new RecordingListener(events));

        Function<ELContext, @Nullable Object> captured = ELCapturedVariables.capture(creating,
            new String[]{"s"}, context -> {
                VariableMapper mapper = context.getVariableMapper();
                assertSame(str, mapper.resolveVariable("s"));
                assertSame(other, mapper.resolveVariable("o"));
                ValueExpression bound = literal("bound");
                assertNull(mapper.setVariable("b", bound));
                assertSame(bound, invoking.getVariableMapper().resolveVariable("b"));

                assertSame(invoking.getELResolver(), context.getELResolver());
                assertSame(invoking.getImportHandler(), context.getImportHandler());
                assertSame(invoking.getFunctionMapper(), context.getFunctionMapper());

                context.putContext(String.class, "object");
                assertEquals("object", invoking.getContext(String.class));
                assertEquals("object", context.getContext(String.class));

                context.setLocale(Locale.ITALIAN);
                assertEquals(Locale.ITALIAN, invoking.getLocale());
                assertEquals(Locale.ITALIAN, context.getLocale());

                context.setPropertyResolved(false);
                context.setPropertyResolved("base", "property");
                assertTrue(invoking.isPropertyResolved());
                assertTrue(context.isPropertyResolved());

                context.notifyBeforeEvaluation("e");
                context.notifyAfterEvaluation("e");
                context.addEvaluationListener(new RecordingListener(events));

                context.enterLambdaScope(Map.of("x", 1));
                assertTrue(invoking.isLambdaArgument("x"));
                assertTrue(context.isLambdaArgument("x"));
                assertEquals(1, context.getLambdaArgument("x"));
                context.exitLambdaScope();
                assertFalse(invoking.isLambdaArgument("x"));

                assertEquals(5, context.convertToType("5", Integer.class));
                return "done";
            });

        assertEquals("done", captured.apply(invoking));
        // setPropertyResolved(base, property) notifies the listeners of the invoking context
        assertEquals(List.of("resolved base.property", "before e", "after e"), events);
        // the listener added through the wrapping context was added to the invoking one, so it is notified twice
        invoking.notifyPropertyResolved("b", "p");
        assertEquals(List.of("resolved base.property", "before e", "after e", "resolved b.p", "resolved b.p"), events);
    }

    @Test
    void aContextWithoutVariablesOnlyAnswersTheCapturedOnes() {
        CompiledELContext creating = new CompiledELContext();
        ValueExpression str = literal("str");
        creating.getVariableMapper().setVariable("s", str);

        Object mapper = ELCapturedVariables.capture(creating, new String[]{"s"}, body).apply(new NoVariablesContext());

        VariableMapper variables = (VariableMapper) mapper;
        assertSame(str, variables.resolveVariable("s"));
        assertNull(variables.resolveVariable("o"));
        assertNull(variables.setVariable("o", literal("dropped")));
        assertNull(variables.resolveVariable("o"));
    }

    @Test
    void notifyingAPropertyResolvedReachesTheListenersOfTheInvokingContext() {
        CompiledELContext creating = new CompiledELContext();
        creating.getVariableMapper().setVariable("s", literal("str"));
        CompiledELContext invoking = new CompiledELContext();
        List<String> events = new ArrayList<>();
        invoking.addEvaluationListener(new RecordingListener(events));

        Object result = ELCapturedVariables.capture(creating, new String[]{"s"}, context -> {
            context.notifyPropertyResolved("base", "property");
            return "notified";
        }).apply(invoking);

        assertEquals("notified", result);

        assertEquals(List.of("resolved base.property"), events);
    }

    private ValueExpression literal(String value) {
        return factory.createValueExpression(value, String.class);
    }

    /**
     * Records the notifications of the evaluation listener.
     */
    private static final class RecordingListener extends EvaluationListener {

        private final List<String> events;

        private RecordingListener(List<String> events) {
            this.events = events;
        }

        @Override
        public void beforeEvaluation(ELContext context, String expression) {
            events.add("before " + expression);
        }

        @Override
        public void afterEvaluation(ELContext context, String expression) {
            events.add("after " + expression);
        }

        @Override
        public void propertyResolved(ELContext context, Object base, Object property) {
            events.add("resolved " + base + "." + property);
        }
    }

    private static final class NoVariablesContext extends ELContext {

        private final CompiledELContext delegate = new CompiledELContext();

        @Override
        public ELResolver getELResolver() {
            return delegate.getELResolver();
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return delegate.getFunctionMapper();
        }

        @Override
        public @Nullable VariableMapper getVariableMapper() {
            return null;
        }
    }
}
