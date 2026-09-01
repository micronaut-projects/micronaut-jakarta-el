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
import io.micronaut.core.type.Argument;
import jakarta.el.ELContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Dispatches the methods every {@link ELMethodContributor} on the classpath registered.
 *
 * <p>This is the executor that brings contributions into the interpreter: it collects the contributors as
 * services once, merges their registrations into one {@link ELMethodRegistry}, and delegates to the executor
 * the registry builds. It runs before the built-in executors, so a contribution takes precedence over the
 * general handling of a type, and well before the reflective executor of the optional interpreter-reflection
 * module.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.1
 */
@Internal
public final class ContributedELMethodExecutor implements ELMethodExecutor {

    /**
     * Contributions describe one type each, so they are consulted before the built-in executors, which
     * describe whole families of types, and before the reflective one, which describes anything.
     */
    public static final int ORDER = -100;

    private final ELMethodExecutor delegate;

    /**
     * Creates the executor from the contributors visible to the context class loader.
     */
    public ContributedELMethodExecutor() {
        this(contribute(load()));
    }

    /**
     * Creates the executor from the given contributors.
     *
     * @param contributors The contributors
     */
    public ContributedELMethodExecutor(List<ELMethodContributor> contributors) {
        this(contribute(contributors));
    }

    private ContributedELMethodExecutor(ELMethodExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    @Nullable
    public ELMethod resolve(ELContext context,
                            @Nullable Object base,
                            @Nullable Object method,
                            Argument<?> @Nullable [] argumentTypes,
                            Object @Nullable [] arguments) {
        return delegate.resolve(context, base, method, argumentTypes, arguments);
    }

    @Override
    @Nullable
    public ELMethod resolveFunction(ELContext context, String prefix, String localName) {
        return delegate.resolveFunction(context, prefix, localName);
    }

    private static ELMethodExecutor contribute(List<ELMethodContributor> contributors) {
        ELMethodRegistry registry = new ELMethodRegistry();
        List<ELMethodContributor> ordered = new ArrayList<>(contributors);
        OrderUtil.sort(ordered);
        for (ELMethodContributor contributor : ordered) {
            contributor.contribute(registry);
        }
        return registry.build(ORDER);
    }

    private static List<ELMethodContributor> load() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return SoftServiceLoader.load(ELMethodContributor.class,
            classLoader == null ? ContributedELMethodExecutor.class.getClassLoader() : classLoader).collectAll();
    }
}
