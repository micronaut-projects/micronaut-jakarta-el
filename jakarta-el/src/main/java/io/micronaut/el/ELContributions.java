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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.el.runtime.ELClassLoaderCache;

import java.util.ArrayList;
import java.util.List;

/**
 * What the {@link ELMethodContributor} services of the classpath registered, merged into one registry.
 *
 * <p>The contributors are collected once and their registrations are read from two places: the executor the
 * interpreter dispatches methods through, and the coercion of a lambda expression to a functional interface,
 * which a compiled expression reaches with no interpreter present. Holding one registry keeps a contributor
 * called once whichever of them runs first.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
public final class ELContributions {

    /**
     * The registry of the contributors each class loader sees, read on first use with the context class loader of
     * the thread rather than once for the JVM with the loader of whichever thread was first.
     */
    private static final ELClassLoaderCache<ELMethodRegistry> REGISTRIES =
        new ELClassLoaderCache<>(ELContributions.class.getClassLoader(), ELContributions::load);

    private ELContributions() {
    }

    /**
     * The registry the contributors visible to the context class loader of the thread declared into.
     *
     * @return The registry
     */
    public static ELMethodRegistry shared() {
        return REGISTRIES.get();
    }

    /**
     * The registry the contributors visible to the given class loader declared into.
     *
     * @param classLoader The class loader
     * @return The registry
     */
    public static ELMethodRegistry shared(ClassLoader classLoader) {
        return REGISTRIES.get(classLoader);
    }

    private static ELMethodRegistry load(ClassLoader classLoader) {
        List<ELMethodContributor> contributors = new ArrayList<>(
            SoftServiceLoader.load(ELMethodContributor.class, classLoader).collectAll());
        OrderUtil.sort(contributors);
        ELMethodRegistry registry = new ELMethodRegistry();
        for (ELMethodContributor contributor : contributors) {
            contributor.contribute(registry);
        }
        return registry;
    }
}
