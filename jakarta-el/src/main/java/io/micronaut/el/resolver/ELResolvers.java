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
package io.micronaut.el.resolver;

import io.micronaut.context.BeanDefinitionRegistry;
import io.micronaut.core.annotation.Experimental;
import jakarta.el.ArrayELResolver;
import jakarta.el.BeanELResolver;
import jakarta.el.ELResolver;
import jakarta.el.ListELResolver;
import jakarta.el.MapELResolver;
import jakarta.el.OptionalELResolver;
import jakarta.el.RecordELResolver;
import jakarta.el.ResourceBundleELResolver;
import jakarta.el.StaticFieldELResolver;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The factory of the standard resolver chain of the module.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public final class ELResolvers {

    /**
     * The resolvers that follow the introspections and precede the reflective invocation, which hold no state
     * and are shared by every chain.
     */
    private static final List<ELResolver> COMPILED = List.of(
        new CommonELResolver(),
        new StreamELResolver()
    );

    /**
     * The resolvers of the specification, from the reflective invocation on, which hold no state and are shared
     * by every chain.
     */
    private static final List<ELResolver> REFLECTIVE = List.of(
        new ReflectiveMethodELResolver(),
        new StaticFieldELResolver(),
        new MapELResolver(),
        new ResourceBundleELResolver(),
        new ListELResolver(),
        new ArrayELResolver(),
        new RecordELResolver(),
        new OptionalELResolver(),
        new BeanELResolver()
    );

    private ELResolvers() {
    }

    /**
     * Creates the standard chain of resolvers, which starts with the bean introspections generated at
     * compilation time and continues with the resolvers of the specification.
     *
     * @return The resolver chain
     */
    public static ELResolver standard() {
        return new ELResolverChain(standardResolvers());
    }

    /**
     * Creates the standard chain of resolvers, starting with the given resolvers.
     *
     * @param first The resolvers to consult first
     * @return The resolver chain
     */
    public static ELResolver standard(ELResolver... first) {
        return new ELResolverChain(standardResolvers(first));
    }

    /**
     * Creates the standard chain of resolvers for an application that has a bean context, which adds, between
     * the introspections and the reflective invocation, the resolver reading the executable methods the beans
     * of that context carry.
     *
     * <p>The registry is passed in rather than read from a static holder, so that an application running more
     * than one bean context resolves a method in the context the expression is evaluated for. A
     * {@code io.micronaut.context.BeanContext} is a registry; the narrower type is what the resolver needs,
     * since it reads definitions and never looks a bean up.</p>
     *
     * @param registry The registry whose definitions carry the executable methods
     * @param first    The resolvers to consult first
     * @return The resolver chain
     */
    public static ELResolver standard(BeanDefinitionRegistry registry, ELResolver... first) {
        return new ELResolverChain(standardResolvers(registry, first));
    }

    /**
     * The resolvers of the standard chain, in order, for a chain that adds resolvers in front of them.
     *
     * @param first The resolvers to consult first
     * @return The resolvers
     */
    public static List<ELResolver> standardResolvers(ELResolver... first) {
        return build(null, first);
    }

    /**
     * The resolvers of the standard chain of an application that has a bean context, in order, for a chain that
     * adds resolvers in front of them.
     *
     * @param registry The registry whose definitions carry the executable methods
     * @param first    The resolvers to consult first
     * @return The resolvers
     */
    public static List<ELResolver> standardResolvers(BeanDefinitionRegistry registry, ELResolver... first) {
        return build(Objects.requireNonNull(registry, "The bean definition registry cannot be null"), first);
    }

    private static List<ELResolver> build(@Nullable BeanDefinitionRegistry registry, ELResolver... first) {
        List<ELResolver> resolvers = new ArrayList<>(first.length + COMPILED.size() + REFLECTIVE.size() + 2);
        resolvers.addAll(List.of(first));
        resolvers.add(new IntrospectionELResolver());
        resolvers.addAll(COMPILED);
        if (registry != null) {
            // an introspection is the more precise description where it exists, and reflection is the last
            // resort, so the executable methods sit between them
            resolvers.add(new ExecutableMethodELExecutor(registry));
        }
        resolvers.addAll(REFLECTIVE);
        return resolvers;
    }
}
