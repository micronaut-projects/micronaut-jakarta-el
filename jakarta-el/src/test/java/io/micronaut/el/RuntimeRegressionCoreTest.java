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

import io.micronaut.el.resolver.ELResolvers;
import io.micronaut.el.runtime.ELClassLoaderCache;
import io.micronaut.el.runtime.ELLambdas;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.BeanELResolver;
import jakarta.el.ELResolver;
import jakarta.el.LambdaExpression;
import org.junit.jupiter.api.Test;

import java.beans.XMLDecoder;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The regressions of the core runtime that are not expressions: the deny list of the sandbox, the caches kept per
 * class loader, and the coercion of a lambda expression called from several threads.
 */
class RuntimeRegressionCoreTest {

    private final ELSandbox sandbox = ELSandbox.standard();

    @Test
    void theSandboxDeniesTheTypesAnApplicationImportReachesBeyondJavaLang() throws Exception {
        assertFalse(sandbox.allowsType(FileInputStream.class));
        assertFalse(sandbox.allowsType(ByteArrayInputStream.class));
        assertFalse(sandbox.allowsType(XMLDecoder.class));
        assertFalse(sandbox.allowsType(InetAddress.class));
        assertFalse(sandbox.allowsType(Connection.class));
        assertFalse(sandbox.allowsType(StackWalker.class));
        assertFalse(sandbox.allowsType(java.lang.invoke.MethodHandles.class));
        assertFalse(sandbox.allowsType(Class.forName("javax.management.MBeanServer")));
        assertFalse(sandbox.allowsType(Class.forName("javax.naming.InitialContext")));
        assertFalse(sandbox.allowsType(Class.forName("javax.script.ScriptEngineManager")));
        assertFalse(sandbox.allowsType(Class.forName("java.lang.management.ManagementFactory")));
        assertFalse(sandbox.allowsType(Class.forName("jdk.internal.misc.Unsafe")));
        assertFalse(sandbox.allowsType(Class.forName("sun.misc.Unsafe")));
        assertFalse(sandbox.allowsType(Class.forName("com.sun.net.httpserver.HttpServer")));
    }

    @Test
    void theSandboxDeniesTheMethodsThatReadTheSystemProperties() {
        assertFalse(sandbox.allowsMethod(Boolean.class, "getBoolean"));
        assertFalse(sandbox.allowsMethod(Integer.class, "getInteger"));
        assertFalse(sandbox.allowsMethod(Long.class, "getLong"));
        assertFalse(sandbox.allowsMethod(ResourceBundle.class, "getBundle"));
        assertTrue(sandbox.allowsMethod(Boolean.class, "valueOf"));
        assertTrue(sandbox.allowsMethod(Integer.class, "parseInt"));
        assertTrue(ELSandbox.UNRESTRICTED.allowsMethod(Boolean.class, "getBoolean"));
    }

    @Test
    void theSandboxJudgesAValueOfThePlatformByItsPublicType() {
        // sun.util.calendar.ZoneInfo and sun.nio.cs.UTF_8, which their modules do not export
        assertTrue(sandbox.allowsType(TimeZone.getTimeZone("UTC").getClass()));
        assertTrue(sandbox.allowsType(StandardCharsets.UTF_8.getClass()));
        assertTrue(sandbox.allowsType(List.of(1, 2, 3).getClass()));
        assertTrue(sandbox.allowsType(IOException.class));
        assertTrue(sandbox.allowsType(UncheckedIOException.class));
        // an implementation of a denied type is denied whatever its package
        assertFalse(sandbox.allowsType(Path.of("x").getClass()));
        assertFalse(sandbox.allowsType(ClassLoader.getSystemClassLoader().getClass()));
    }

    @Test
    void aCacheKeepsOneValuePerClassLoader() throws Exception {
        AtomicInteger reads = new AtomicInteger();
        ClassLoader parent = getClass().getClassLoader();
        ELClassLoaderCache<Object> cache = new ELClassLoaderCache<>(parent, loader -> {
            reads.incrementAndGet();
            return new Object();
        });
        try (URLClassLoader child = new URLClassLoader(new URL[0], parent)) {
            Object first = cache.get(parent);

            assertSame(first, cache.get(parent));
            assertNotSame(first, cache.get(child));
            assertSame(cache.get(child), cache.get(child));
            assertSame(first, withContextClassLoader(null, cache::get));
            assertSame(cache.get(child), withContextClassLoader(child, cache::get));
            assertEquals(2, reads.get());
        }
    }

    @Test
    void theContributionsAreReadWithTheContextClassLoaderOfEachCaller() throws Exception {
        ClassLoader parent = getClass().getClassLoader();
        try (URLClassLoader child = new URLClassLoader(new URL[]{root("child-loader-contributor")}, parent)) {
            // the first caller is the child loader: the parent must not keep what the child sees
            assertNotNull(withContextClassLoader(child,
                () -> ELContributions.shared().functionalInterface(ChildLoaderContributor.Shout.class)));
            assertNull(withContextClassLoader(parent,
                () -> ELContributions.shared().functionalInterface(ChildLoaderContributor.Shout.class)));
        }
    }

    @Test
    void theStandardResolversAreSharedPerClassLoader() throws Exception {
        ClassLoader parent = getClass().getClassLoader();
        ELResolver first = beanResolver(withContextClassLoader(parent, () -> ELResolvers.standardResolvers()));
        ELResolver second = beanResolver(withContextClassLoader(parent, () -> ELResolvers.standardResolvers()));

        assertSame(first, second, "a new context must not read the classpath again");
        try (URLClassLoader child = new URLClassLoader(new URL[0], parent)) {
            assertNotSame(first, beanResolver(withContextClassLoader(child, () -> ELResolvers.standardResolvers())),
                "the resolver of the beans caches classes, so a loader must not share another one's");
        }
    }

    @Test
    void aCoercedLambdaIsSafeToCallFromSeveralThreads() {
        CompiledELContext context = new CompiledELContext().setBean("offset", 1L);
        LambdaExpression lambda = ELLambdas.create(context, List.of("x"), evaluated ->
            ((Number) evaluated.getLambdaArgument("x")).longValue()
                + (Long) evaluated.getELResolver().getValue(evaluated, null, "offset"));
        @SuppressWarnings("unchecked")
        Function<Object, Object> function = ELSupport.coerceToType(context, lambda, Function.class);
        List<Integer> values = IntStream.range(0, 50_000).boxed().collect(Collectors.toList());

        for (int i = 0; i < 5; i++) {
            List<Object> mapped = values.parallelStream().map(function).collect(Collectors.toList());
            List<Object> expected = new ArrayList<>(values.size());
            values.forEach(value -> expected.add(value + 1L));
            assertEquals(expected, mapped);
        }
    }

    @Test
    void aStringWithoutAnEditorIsNotCoercedToAnArbitraryType() {
        assertNull(ELSupport.coerce("", RuntimeRegressionCoreTest.class));
        jakarta.el.ELException failure = org.junit.jupiter.api.Assertions.assertThrows(jakarta.el.ELException.class,
            () -> ELSupport.coerce("x", RuntimeRegressionCoreTest.class));
        assertTrue(failure.getMessage().contains(RuntimeRegressionCoreTest.class.getName()), failure.getMessage());
    }

    private static ELResolver beanResolver(List<ELResolver> resolvers) {
        for (ELResolver resolver : resolvers) {
            if (resolver instanceof BeanELResolver) {
                return resolver;
            }
        }
        throw new AssertionError("no BeanELResolver in " + resolvers);
    }

    private URL root(String name) throws Exception {
        URL marker = getClass().getResource("/" + name + "/marker");
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
}
