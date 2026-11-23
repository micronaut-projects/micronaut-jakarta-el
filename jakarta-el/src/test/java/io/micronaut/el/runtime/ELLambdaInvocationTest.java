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
package io.micronaut.el.runtime;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.EvaluationListener;
import jakarta.el.FunctionMapper;
import jakarta.el.LambdaExpression;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The contract of the context an invocation of a lambda expression through a functional interface is given, and
 * of the variables a lambda expression closes over. These are Java API contracts with no expression text of their
 * own, so they have no generated twin.
 */
class ELLambdaInvocationTest {

    @Test
    void aCompiledLambdaInvokedWithoutAContextUsesAnIsolatedCopyOfItsCreationContext() {
        CompiledELContext creation = new CompiledELContext();
        creation.putContext(String.class, "creation");
        AtomicReference<ELContext> seen = new AtomicReference<>();
        CompiledLambdaExpression lambda = new CompiledLambdaExpression(creation, new String[]{"x"},
            (context, arguments) -> {
                seen.set(context);
                return context.getContext(String.class) + ":" + arguments[0];
            });

        assertEquals("creation:1", ELLambdaInvocation.invoke(null, lambda, 1));
        assertNotSame(creation, seen.get());
    }

    @Test
    void anotherLambdaInvokedWithoutAContextUsesTheContextItCarries() {
        LambdaExpression lambda = new LambdaExpression(List.of(), literal("carried"));
        lambda.setELContext(new CompiledELContext());

        assertEquals("carried", ELLambdaInvocation.invoke(null, lambda));
    }

    @Test
    void anIsolatedContextReadsItsParentButKeepsItsOwnScopeFlagLocaleAndObjects() {
        CompiledELContext parent = new CompiledELContext();
        parent.setLocale(Locale.FRENCH);
        parent.putContext(String.class, "parent");
        List<String> events = new ArrayList<>();
        parent.addEvaluationListener(new EvaluationListener() {
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
        });
        Map<String, Object> outer = new HashMap<>();
        outer.put("outer", "o");
        parent.enterLambdaScope(outer);

        ELContext isolated = ELLambdaInvocation.isolate(parent);

        assertSame(parent.getELResolver(), isolated.getELResolver());
        assertSame(parent.getFunctionMapper(), isolated.getFunctionMapper());
        assertSame(parent.getImportHandler(), isolated.getImportHandler());
        assertSame(parent.getVariableMapper(), isolated.getVariableMapper());

        assertEquals(Locale.FRENCH, isolated.getLocale());
        isolated.setLocale(Locale.GERMAN);
        assertEquals(Locale.GERMAN, isolated.getLocale());
        assertEquals(Locale.FRENCH, parent.getLocale());

        assertEquals("parent", isolated.getContext(String.class));
        isolated.putContext(String.class, "own");
        assertEquals("own", isolated.getContext(String.class));
        assertEquals("parent", parent.getContext(String.class));

        isolated.setPropertyResolved(true);
        assertFalse(parent.isPropertyResolved());

        isolated.notifyBeforeEvaluation("e");
        isolated.notifyPropertyResolved("b", "p");
        isolated.notifyAfterEvaluation("e");
        assertEquals(List.of("before e", "resolved b.p", "after e"), events);

        // the arguments of the enclosing lambda expressions are read from the parent, the own ones shadow them
        assertFalse(isolated.isLambdaArgument("x"));
        isolated.exitLambdaScope();
        isolated.enterLambdaScope(Map.of("x", 1, "outer", "shadowed"));
        isolated.enterLambdaScope(Map.of("y", 2));
        assertTrue(isolated.isLambdaArgument("x"));
        assertTrue(isolated.isLambdaArgument("outer"));
        assertEquals(1, isolated.getLambdaArgument("x"));
        assertEquals(2, isolated.getLambdaArgument("y"));
        assertEquals("shadowed", isolated.getLambdaArgument("outer"));
        isolated.exitLambdaScope();
        isolated.exitLambdaScope();
        assertFalse(isolated.isLambdaArgument("x"));
        assertTrue(isolated.isLambdaArgument("outer"));
        assertEquals("o", isolated.getLambdaArgument("outer"));
        assertNull(isolated.getLambdaArgument("x"));
        // nothing was written to the parent
        assertFalse(parent.isLambdaArgument("x"));
    }

    @Test
    void aSnapshotOfAMapperThisRuntimeCannotListResolvesTheFreeNamesWhenItIsTaken() {
        RecordingMapper mapper = new RecordingMapper();
        ValueExpression first = literal("first");
        mapper.setVariable("v", first);

        assertNull(ELLambdaInvocation.snapshot(null));
        assertNull(ELLambdaInvocation.snapshot(null, new String[]{"v"}));
        assertSame(mapper, ELLambdaInvocation.snapshot(mapper));
        assertSame(mapper, ELLambdaInvocation.snapshot(mapper, new String[0]));

        VariableMapper snapshot = ELLambdaInvocation.snapshot(mapper, new String[]{"v", "unbound"});
        mapper.setVariable("v", literal("second"));

        assertSame(first, snapshot.resolveVariable("v"));
        assertNull(snapshot.resolveVariable("unbound"));
    }

    @Test
    void theVariablesALambdaClosedOverResolveBeforeTheOnesOfTheInvokingContext() {
        CompiledELContext creation = new CompiledELContext();
        ValueExpression closedValue = literal("closed");
        creation.getVariableMapper().setVariable("v", closedValue);
        VariableMapper closed = ELLambdaInvocation.snapshot(creation.getVariableMapper());

        CompiledELContext invoking = new CompiledELContext();
        ValueExpression ownValue = literal("own");
        invoking.getVariableMapper().setVariable("w", ownValue);
        ELContext capturing = ELLambdaInvocation.capturing(invoking, closed);
        VariableMapper mapper = capturing.getVariableMapper();

        assertSame(closedValue, mapper.resolveVariable("v"));
        assertSame(ownValue, mapper.resolveVariable("w"));
        assertNull(mapper.resolveVariable("none"));
        // a binding is written to the invoking context
        ValueExpression bound = literal("bound");
        assertNull(mapper.setVariable("x", bound));
        assertSame(bound, invoking.getVariableMapper().resolveVariable("x"));
        // the mapper of the creation context itself is not wrapped
        assertSame(creation.getVariableMapper(),
            ELLambdaInvocation.capturing(creation, creation.getVariableMapper()).getVariableMapper());

        // a snapshot of the captured variables stays current until the invoking context binds another variable
        VariableMapper snapshot = ELLambdaInvocation.snapshot(mapper);
        assertTrue(ELLambdaInvocation.isCurrent(snapshot, capturing.getVariableMapper()));
        invoking.getVariableMapper().setVariable("y", literal("later"));
        assertFalse(ELLambdaInvocation.isCurrent(snapshot, capturing.getVariableMapper()));
        // another lambda expression closed over other variables
        VariableMapper other = ELLambdaInvocation.capturing(invoking, ELLambdaInvocation.snapshot(
            invoking.getVariableMapper())).getVariableMapper();
        assertFalse(ELLambdaInvocation.isCurrent(snapshot, other));
    }

    @Test
    void aSnapshotIsNeverCurrentAgainstAMissingMapper() {
        VariableMapper snapshot = ELLambdaInvocation.snapshot(new CompiledELContext().getVariableMapper());

        assertTrue(ELLambdaInvocation.isCurrent(null, null));
        assertFalse(ELLambdaInvocation.isCurrent(snapshot, null));
        assertFalse(ELLambdaInvocation.isCurrent(null, snapshot));
    }

    @Test
    void aContextWithoutVariablesOnlyResolvesTheClosedOverOnesAndDropsBindings() {
        CompiledELContext creation = new CompiledELContext();
        ValueExpression closedValue = literal("closed");
        creation.getVariableMapper().setVariable("v", closedValue);
        ELContext capturing = ELLambdaInvocation.capturing(new NoVariablesContext(),
            ELLambdaInvocation.snapshot(creation.getVariableMapper()));
        VariableMapper mapper = capturing.getVariableMapper();

        assertSame(closedValue, mapper.resolveVariable("v"));
        assertNull(mapper.resolveVariable("w"));
        assertNull(mapper.setVariable("w", literal("dropped")));
        assertNull(mapper.resolveVariable("w"));
    }

    private static ValueExpression literal(String value) {
        return new LiteralValueExpression(value);
    }

    /**
     * A variable mapper this runtime cannot list, such as the one of a {@code StandardELContext}.
     */
    private static final class RecordingMapper extends VariableMapper {

        private final Map<String, ValueExpression> variables = new HashMap<>();

        @Override
        public @Nullable ValueExpression resolveVariable(String variable) {
            return variables.get(variable);
        }

        @Override
        public @Nullable ValueExpression setVariable(String variable, @Nullable ValueExpression expression) {
            return variables.put(variable, expression);
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

    private static final class LiteralValueExpression extends ValueExpression {

        private static final long serialVersionUID = 1L;

        private final String value;

        private LiteralValueExpression(String value) {
            this.value = value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getValue(ELContext context) {
            return (T) value;
        }

        @Override
        public void setValue(ELContext context, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isReadOnly(ELContext context) {
            return true;
        }

        @Override
        public Class<?> getType(ELContext context) {
            return String.class;
        }

        @Override
        public Class<?> getExpectedType() {
            return String.class;
        }

        @Override
        public String getExpressionString() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof LiteralValueExpression other && other.value.equals(value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean isLiteralText() {
            return true;
        }
    }
}
