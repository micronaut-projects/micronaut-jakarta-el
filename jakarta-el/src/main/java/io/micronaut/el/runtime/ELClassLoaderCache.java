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

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * What the module reads from the classpath once per class loader: the services a class loader sees, which a
 * static holder would otherwise read with the context class loader of whichever thread happened to be first,
 * and keep for every other loader of the JVM.
 *
 * <p>The class loader is held weakly and the value softly: a value holds classes of its loader, so a strongly
 * held value would keep a loader that was otherwise dropped, a redeployed application or a closed plugin,
 * reachable from this cache forever. A value the collector cleared is read again on the next use.</p>
 *
 * <p>A read does not lock: the entries are an immutable array replaced as a whole on every write, and there are
 * only ever as many of them as there are class loaders evaluating expressions.</p>
 *
 * @param <T> The type of the value
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
public final class ELClassLoaderCache<T> {

    private final Function<ClassLoader, T> reader;
    private final ClassLoader fallback;

    @SuppressWarnings("unchecked")
    private volatile Entry<T>[] entries = new Entry[0];

    /**
     * @param fallback The class loader used when the thread has no context class loader, normally the loader of
     *                 the class owning the cache
     * @param reader   Reads the value of a class loader
     */
    public ELClassLoaderCache(ClassLoader fallback, Function<ClassLoader, T> reader) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.reader = Objects.requireNonNull(reader, "reader");
    }

    /**
     * @return The value of the context class loader of the current thread, or of the fallback loader when the
     * thread has none
     */
    public T get() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return get(classLoader == null ? fallback : classLoader);
    }

    /**
     * @param classLoader The class loader
     * @return The value of the class loader, read once and kept until the loader or the value is collected
     */
    public T get(ClassLoader classLoader) {
        T value = find(entries, classLoader);
        if (value != null) {
            return value;
        }
        synchronized (this) {
            value = find(entries, classLoader);
            if (value != null) {
                return value;
            }
            value = Objects.requireNonNull(reader.apply(classLoader), "The value of a class loader cannot be null");
            List<Entry<T>> retained = new ArrayList<>(entries.length + 1);
            for (Entry<T> entry : entries) {
                ClassLoader key = entry.classLoader.get();
                // the entries of a collected loader or value, and the stale one of this loader, are dropped
                if (key != null && key != classLoader && entry.value.get() != null) {
                    retained.add(entry);
                }
            }
            retained.add(new Entry<>(classLoader, value));
            @SuppressWarnings("unchecked")
            Entry<T>[] replaced = retained.toArray(new Entry[0]);
            entries = replaced;
            return value;
        }
    }

    @Nullable
    private static <T> T find(Entry<T>[] entries, ClassLoader classLoader) {
        for (Entry<T> entry : entries) {
            if (entry.classLoader.get() == classLoader) {
                return entry.value.get();
            }
        }
        return null;
    }

    /**
     * A class loader, held weakly, and its value, held softly.
     *
     * @param <T> The type of the value
     */
    private static final class Entry<T> {

        private final WeakReference<ClassLoader> classLoader;
        private final SoftReference<T> value;

        private Entry(ClassLoader classLoader, T value) {
            this.classLoader = new WeakReference<>(classLoader);
            this.value = new SoftReference<>(value);
        }
    }
}
