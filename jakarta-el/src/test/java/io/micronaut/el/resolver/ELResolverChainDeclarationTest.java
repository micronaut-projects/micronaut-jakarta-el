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

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The chain declares which methods each resolver leaves to {@link ELResolver} rather than reading it from the
 * class, so that the runtime of this module does not reflect. This test does the reading the runtime no longer
 * does, and holds the declaration to it.
 */
class ELResolverChainDeclarationTest {

    private static final List<Class<?>> RESOLVERS = List.of(
        CommonELResolver.class, IntrospectionELResolver.class, StreamELResolver.class,
        jakarta.el.StaticFieldELResolver.class, jakarta.el.MapELResolver.class,
        jakarta.el.ResourceBundleELResolver.class, jakarta.el.ListELResolver.class,
        jakarta.el.ArrayELResolver.class, jakarta.el.RecordELResolver.class,
        jakarta.el.OptionalELResolver.class, jakarta.el.BeanELResolver.class);

    @Test
    void whatTheChainDeclaresIsWhatTheResolversOverride() {
        for (Class<?> resolver : RESOLVERS) {
            assertEquals(overrides(resolver, "convertToType", ELContext.class, Object.class, Class.class),
                ELResolverChain.overridesConvertToType(resolver), resolver.getName() + ".convertToType");
            assertEquals(overrides(resolver, "invoke", ELContext.class, Object.class, Object.class,
                    Class[].class, Object[].class),
                ELResolverChain.overridesInvoke(resolver), resolver.getName() + ".invoke");
        }
    }

    private static boolean overrides(Class<?> resolver, String name, Class<?>... parameterTypes) {
        try {
            Method method = resolver.getMethod(name, parameterTypes);
            return method.getDeclaringClass() != ELResolver.class;
        } catch (NoSuchMethodException e) {
            return true;
        }
    }
}
