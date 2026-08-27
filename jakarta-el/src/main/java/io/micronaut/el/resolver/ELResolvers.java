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

import java.util.ArrayList;
import java.util.List;

/**
 * The factory of the standard resolver chain of the module.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public final class ELResolvers {

    /**
     * The resolvers of the specification that follow the introspections, which hold no state and are shared by
     * every chain.
     */
    private static final List<ELResolver> SPECIFICATION = List.of(
        new StreamELResolver(),
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
     * The resolvers of the standard chain, in order, for a chain that adds resolvers in front of them.
     *
     * @param first The resolvers to consult first
     * @return The resolvers
     */
    public static List<ELResolver> standardResolvers(ELResolver... first) {
        List<ELResolver> resolvers = new ArrayList<>(first.length + SPECIFICATION.size() + 1);
        resolvers.addAll(List.of(first));
        resolvers.add(new IntrospectionELResolver());
        resolvers.addAll(SPECIFICATION);
        return resolvers;
    }
}
