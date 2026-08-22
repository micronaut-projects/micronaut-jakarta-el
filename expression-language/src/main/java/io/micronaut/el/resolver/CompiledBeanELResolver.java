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

import org.jspecify.annotations.Nullable;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.PropertyNotWritableException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link ELResolver} resolving the types annotated with {@code io.micronaut.el.annotation.ELBean}
 * through the resolvers generated at compilation time.
 *
 * <p>The resolution never uses reflection. Types that have no generated resolver are left unresolved, so
 * that the rest of the resolver chain can handle them.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class CompiledBeanELResolver extends ELResolver {

    private final Map<Class<?>, ELBeanResolver> resolvers;
    private final Map<Class<?>, ELBeanResolver> resolved = new ConcurrentHashMap<>();
    private final boolean readOnly;

    /**
     * Creates a resolver loading the generated bean resolvers with the {@link SoftServiceLoader}.
     */
    public CompiledBeanELResolver() {
        this(false);
    }

    /**
     * @param readOnly Whether the resolver should reject the assignment of properties
     */
    public CompiledBeanELResolver(boolean readOnly) {
        this(load(CompiledBeanELResolver.class.getClassLoader()), readOnly);
    }

    /**
     * @param beanResolvers The generated bean resolvers
     * @param readOnly      Whether the resolver should reject the assignment of properties
     */
    public CompiledBeanELResolver(List<ELBeanResolver> beanResolvers, boolean readOnly) {
        Map<Class<?>, ELBeanResolver> byType = new HashMap<>(beanResolvers.size());
        for (ELBeanResolver beanResolver : beanResolvers) {
            byType.put(beanResolver.getBeanType(), beanResolver);
        }
        this.resolvers = byType;
        this.readOnly = readOnly;
    }

    private static List<ELBeanResolver> load(ClassLoader classLoader) {
        return SoftServiceLoader.load(ELBeanResolver.class, classLoader).collectAll();
    }

    @Override
    @Nullable
    public Object getValue(ELContext context, Object base, Object property) {
        ELBeanResolver resolver = find(base, property);
        if (resolver == null) {
            return null;
        }
        String name = ELSupport.coerceToString(property);
        if (!resolver.getPropertyNames().contains(name)) {
            return null;
        }
        context.setPropertyResolved(base, property);
        return resolver.getProperty(context, base, name);
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext context, Object base, Object property) {
        ELBeanResolver resolver = find(base, property);
        if (resolver == null) {
            return null;
        }
        String name = ELSupport.coerceToString(property);
        Class<?> type = resolver.getPropertyType(name);
        if (type == null) {
            return null;
        }
        context.setPropertyResolved(base, property);
        return readOnly || resolver.isReadOnly(name) ? null : type;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        ELBeanResolver resolver = find(base, property);
        if (resolver == null) {
            return;
        }
        String name = ELSupport.coerceToString(property);
        if (!resolver.getPropertyNames().contains(name)) {
            return;
        }
        if (readOnly || resolver.isReadOnly(name)) {
            throw new PropertyNotWritableException("The property '" + name + "' of "
                + base.getClass().getName() + " is not writable");
        }
        context.setPropertyResolved(base, property);
        resolver.setProperty(context, base, name, value);
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        ELBeanResolver resolver = find(base, property);
        if (resolver == null) {
            return false;
        }
        String name = ELSupport.coerceToString(property);
        if (!resolver.getPropertyNames().contains(name)) {
            return false;
        }
        context.setPropertyResolved(base, property);
        return readOnly || resolver.isReadOnly(name);
    }

    @Override
    @Nullable
    public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
        ELBeanResolver resolver = find(base, method);
        if (resolver == null) {
            return null;
        }
        Object[] arguments = params == null ? new Object[0] : params;
        String name = ELSupport.coerceToString(method);
        if (!resolver.hasMethod(name, arguments.length)) {
            return null;
        }
        context.setPropertyResolved(base, method);
        return resolver.invokeMethod(context, base, name, arguments);
    }

    @Override
    @Nullable
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return base != null && find(base, "") != null ? Object.class : null;
    }

    @Nullable
    private ELBeanResolver find(@Nullable Object base, @Nullable Object property) {
        if (base == null || property == null) {
            return null;
        }
        Class<?> type = base.getClass();
        ELBeanResolver resolver = resolved.get(type);
        if (resolver != null) {
            return resolver;
        }
        resolver = findInHierarchy(type);
        if (resolver != null) {
            resolved.put(type, resolver);
        }
        return resolver;
    }

    @Nullable
    private ELBeanResolver findInHierarchy(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            ELBeanResolver resolver = resolvers.get(current);
            if (resolver != null) {
                return resolver;
            }
            for (Class<?> anInterface : current.getInterfaces()) {
                ELBeanResolver interfaceResolver = resolvers.get(anInterface);
                if (interfaceResolver != null) {
                    return interfaceResolver;
                }
            }
        }
        return null;
    }
}
