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

    private ELContributions() {
    }

    /**
     * The registry the contributors of the classpath declared into.
     *
     * @return The registry
     */
    public static ELMethodRegistry shared() {
        return Holder.REGISTRY;
    }

    /**
     * Loaded on first use, so that nothing is read from the classpath until an expression needs it.
     */
    private static final class Holder {

        private static final ELMethodRegistry REGISTRY = load();

        private static ELMethodRegistry load() {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            List<ELMethodContributor> contributors = new ArrayList<>(
                SoftServiceLoader.load(ELMethodContributor.class,
                    classLoader == null ? ELContributions.class.getClassLoader() : classLoader).collectAll());
            OrderUtil.sort(contributors);
            ELMethodRegistry registry = new ELMethodRegistry();
            for (ELMethodContributor contributor : contributors) {
                contributor.contribute(registry);
            }
            return registry;
        }
    }
}
