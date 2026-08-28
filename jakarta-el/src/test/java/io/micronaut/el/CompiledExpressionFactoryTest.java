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
package io.micronaut.el;

import io.micronaut.el.runtime.ObjectValueExpression;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompiledExpressionFactoryTest {

    private final CompiledELContext context = new CompiledELContext();

    @Test
    void theSourcesAreLoadedWithTheContextClassLoader() throws Exception {
        // ExpressionFactory.newInstance() locates the factory with the context class loader of the thread, so a
        // deployment, a plugin or an isolated test holding generated sources in a child loader expects the
        // factory to look there too.
        ClassLoader parent = getClass().getClassLoader();
        try (URLClassLoader child = new URLClassLoader(new URL[] {childLoaderRoot()}, parent)) {
            CompiledExpressionFactory factory = withContextClassLoader(child, CompiledExpressionFactory::new);

            ValueExpression expression = factory.createValueExpression(context, ChildLoaderExpressionSource.EXPRESSION, String.class);

            assertEquals("child", expression.getValue(context));
        }
    }

    @Test
    void aSourceOfAChildLoaderIsNotSeenFromTheParent() throws Exception {
        ClassLoader parent = getClass().getClassLoader();
        CompiledExpressionFactory factory = withContextClassLoader(parent, CompiledExpressionFactory::new);

        ELException e = assertThrows(ELException.class,
            () -> factory.createValueExpression(context, ChildLoaderExpressionSource.EXPRESSION, String.class));

        assertTrue(e.getMessage().contains("was not compiled"));
    }

    @Test
    void theClassLoaderCanBeGivenExplicitly() throws Exception {
        ClassLoader parent = getClass().getClassLoader();
        try (URLClassLoader child = new URLClassLoader(new URL[] {childLoaderRoot()}, parent)) {
            // the context class loader is the parent, the explicit one wins
            CompiledExpressionFactory factory = withContextClassLoader(parent, () -> new CompiledExpressionFactory(child));

            assertEquals("child",
                factory.createValueExpression(context, ChildLoaderExpressionSource.EXPRESSION, String.class).getValue(context));
        }
    }

    @Test
    void theLoaderOfTheFactoryIsUsedWhenTheThreadHasNone() {
        CompiledExpressionFactory factory = withContextClassLoader(null, CompiledExpressionFactory::new);

        // nothing is compiled in this module, the literal proves the factory is usable
        assertEquals("literal", factory.createValueExpression(context, "literal", String.class).getValue(context));
    }

    @Test
    void onlyTheSourcesDeclaringTheExpressionAreConsulted() {
        CountingSource declaring = new CountingSource(List.of("${a}"), "a");
        CountingSource other = new CountingSource(List.of("${b}"), "b");
        CompiledExpressionFactory factory = new CompiledExpressionFactory(List.of(declaring, other));

        assertEquals("a", factory.createValueExpression(context, "${a}", String.class).getValue(context));

        assertEquals(1, declaring.consulted);
        assertEquals(0, other.consulted);
    }

    @Test
    void aSourceDeclaringNoExpressionIsConsultedForEveryExpression() {
        CountingSource declaring = new CountingSource(List.of("${a}"), "a");
        CountingSource legacy = new CountingSource(List.of(), "legacy");
        CompiledExpressionFactory factory = new CompiledExpressionFactory(List.of(declaring, legacy));

        assertEquals("a", factory.createValueExpression(context, "${a}", String.class).getValue(context));
        assertEquals("legacy", factory.createValueExpression(context, "${anything}", String.class).getValue(context));

        // the declaring source answers ${a} before the legacy one is reached, the legacy one answers the rest
        assertEquals(1, declaring.consulted);
        assertEquals(1, legacy.consulted);
    }

    @Test
    void theFirstSourceDeclaringTheExpressionWins() {
        CountingSource first = new CountingSource(List.of("${a}"), "first");
        CountingSource second = new CountingSource(List.of("${a}"), "second");
        CompiledExpressionFactory factory = new CompiledExpressionFactory(List.of(first, second));

        assertEquals("first", factory.createValueExpression(context, "${a}", String.class).getValue(context));
        assertEquals(0, second.consulted);
    }

    @Test
    void theIndexedSourcesComeBeforeTheUnindexedOnes() {
        CountingSource legacy = new CountingSource(List.of(), "legacy");
        CountingSource declaring = new CountingSource(List.of("${a}"), "a");
        // the legacy source is loaded first, the declaring one is still asked first
        CompiledExpressionFactory factory = new CompiledExpressionFactory(List.of(legacy, declaring));

        assertEquals("a", factory.createValueExpression(context, "${a}", String.class).getValue(context));
        assertEquals(0, legacy.consulted);
    }

    @Test
    void theFactoryOfTheServiceIsTheCompiledOne() {
        assertSame(CompiledExpressionFactory.class, ExpressionFactory.newInstance().getClass());
    }

    @Test
    void objectValueExpressionsPreserveSerializableValues() throws Exception {
        ValueExpression expression = new CompiledExpressionFactory().createValueExpression("value", String.class);

        assertEquals("value", roundTrip(expression).getValue(context));
    }

    @Test
    void literalMethodExpressionsHaveNoMethodReference() {
        MethodExpression expression = new CompiledExpressionFactory().createMethodExpression(context, "literal",
            String.class, new Class<?>[0]);

        assertNull(expression.getMethodReference(context));
    }

    private static ValueExpression roundTrip(ValueExpression expression) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(expression);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (ValueExpression) input.readObject();
        }
    }

    private URL childLoaderRoot() throws Exception {
        URL marker = getClass().getResource("/child-loader/marker");
        return Path.of(marker.toURI()).getParent().toUri().toURL();
    }

    private static <T> T withContextClassLoader(ClassLoader classLoader, Supplier<T> supplier) {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try {
            return supplier.get();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    /**
     * A source counting how many times the factory consults it.
     */
    private static final class CountingSource implements ELExpressionSource {

        private final List<String> expressions;
        private final String value;
        private int consulted;

        CountingSource(List<String> expressions, String value) {
            this.expressions = expressions;
            this.value = value;
        }

        @Override
        public List<String> expressions() {
            return expressions;
        }

        @Override
        public ValueExpression createValueExpression(String expression, Class<?> expectedType) {
            consulted++;
            return new ObjectValueExpression(value, expectedType);
        }
    }
}
