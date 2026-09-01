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

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanMethod;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.type.Argument;
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.runtime.ELArguments;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.PropertyNotWritableException;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link ELResolver} resolving the types annotated with {@link io.micronaut.core.annotation.Introspected}
 * through the bean introspection generated for them at compilation time.
 *
 * <p>The properties are read and written through the generated {@code BeanIntrospection}, so no reflection is
 * used. The methods annotated with {@code io.micronaut.context.annotation.Executable} are invoked the same way;
 * the other methods are left to the rest of the chain. A type without an introspection is left unresolved, so
 * that the standard resolvers of the specification can handle it.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class IntrospectionELResolver extends ELResolver implements ELMethodExecutor {

    /**
     * What the resolver needs of a class, read once: the introspection, its properties by name and its methods
     * by name. {@code BeanIntrospector.findIntrospection} loads the introspection through an {@code Optional}
     * on every call, and the methods of a name are otherwise filtered from all the methods on every invocation.
     */
    private final ClassValue<Introspected> introspected = new ClassValue<>() {
        @Override
        protected Introspected computeValue(Class<?> type) {
            return Introspected.of(findIntrospection(type));
        }
    };

    private final BeanIntrospector introspector;
    private final boolean readOnly;

    /**
     * Creates a resolver using the shared introspector.
     */
    public IntrospectionELResolver() {
        this(BeanIntrospector.SHARED, false);
    }

    /**
     * @param readOnly Whether the resolver should reject the assignment of properties
     */
    public IntrospectionELResolver(boolean readOnly) {
        this(BeanIntrospector.SHARED, readOnly);
    }

    /**
     * @param introspector The introspector
     * @param readOnly     Whether the resolver should reject the assignment of properties
     */
    public IntrospectionELResolver(BeanIntrospector introspector, boolean readOnly) {
        this.introspector = introspector;
        this.readOnly = readOnly;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    @Nullable
    public Object getValue(ELContext context, @Nullable Object base, @Nullable Object property) {
        BeanProperty<Object, Object> beanProperty = findProperty(base, property);
        if (base == null || beanProperty == null || beanProperty.isWriteOnly()) {
            return null;
        }
        context.setPropertyResolved(base, property);
        return beanProperty.get(base);
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext context, @Nullable Object base, @Nullable Object property) {
        BeanProperty<Object, Object> beanProperty = findProperty(base, property);
        if (beanProperty == null) {
            return null;
        }
        context.setPropertyResolved(base, property);
        // a read only property has no type to assign to, as described for setValue in the specification
        return isReadOnly(beanProperty) ? null : beanProperty.getType();
    }

    @Override
    public void setValue(ELContext context, @Nullable Object base, @Nullable Object property, @Nullable Object value) {
        BeanProperty<Object, Object> beanProperty = findProperty(base, property);
        if (base == null || beanProperty == null) {
            return;
        }
        if (isReadOnly(beanProperty)) {
            throw new PropertyNotWritableException("The property '" + property + "' of "
                + beanProperty.getDeclaringType().getName() + " is not writable");
        }
        context.setPropertyResolved(base, property);
        beanProperty.set(base, ELSupport.coerceToType(context, value, beanProperty.getType()));
    }

    @Override
    public boolean isReadOnly(ELContext context, @Nullable Object base, @Nullable Object property) {
        BeanProperty<Object, Object> beanProperty = findProperty(base, property);
        if (beanProperty == null) {
            return true;
        }
        context.setPropertyResolved(base, property);
        return isReadOnly(beanProperty);
    }

    @Override
    @Nullable
    public Object invoke(ELContext context,
                         @Nullable Object base,
                         @Nullable Object method,
                         Class<?> @Nullable [] paramTypes,
                         Object @Nullable [] params) {
        ELMethod resolved = resolve(context, base, method, ELArguments.of(paramTypes), params);
        if (resolved == null) {
            return null;
        }
        context.setPropertyResolved(base, method);
        return resolved.invoke(context, base, params);
    }

    @Override
    @Nullable
    public ELMethod resolve(ELContext context,
                            @Nullable Object base,
                            @Nullable Object method,
                            Argument<?> @Nullable [] argumentTypes,
                            Object @Nullable [] arguments) {
        if (base == null || !(method instanceof String name)) {
            return null;
        }
        BeanMethod<Object, Object>[] named = introspected(base.getClass()).methods().get(name);
        if (named == null) {
            return null;
        }
        Object[] values = arguments == null ? new Object[0] : arguments;
        List<BeanMethod<Object, Object>> overloads = List.of(named);
        BeanMethod<Object, Object> selected = argumentTypes == null
            ? ELOverloads.select(context, overloads, BeanMethod::getArguments, values)
            : ELOverloads.declaring(context, overloads, BeanMethod::getArguments, argumentTypes, values);
        return selected == null ? null : new IntrospectionMethod(selected);
    }

    @Override
    @Nullable
    public Class<?> getCommonPropertyType(ELContext context, @Nullable Object base) {
        return base == null ? null : Object.class;
    }

    @Nullable
    private BeanProperty<Object, Object> findProperty(@Nullable Object base, @Nullable Object property) {
        if (base == null || !(property instanceof String name)) {
            return null;
        }
        return introspected(base.getClass()).properties().get(name);
    }

    private Introspected introspected(Class<?> type) {
        return introspected.get(type);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private BeanIntrospection<Object> findIntrospection(Class<?> type) {
        return introspector.findIntrospection((Class<Object>) type).orElse(null);
    }

    private boolean isReadOnly(BeanProperty<Object, Object> beanProperty) {
        return readOnly || beanProperty.isReadOnly();
    }

    private static final class IntrospectionMethod implements ELMethod {
        private final BeanMethod<Object, Object> method;

        private IntrospectionMethod(BeanMethod<Object, Object> method) {
            this.method = method;
        }

        @Override
        public String getName() {
            return method.getName();
        }

        @Override
        public Argument<?> getReturnType() {
            return method.getReturnType().asArgument();
        }

        @Override
        public Argument<?>[] getArguments() {
            return method.getArguments().clone();
        }

        @Override
        public boolean isVarArgs() {
            return false;
        }

        @Override
        public AnnotationMetadata getAnnotationMetadata() {
            return method.getAnnotationMetadata();
        }

        @Override
        @Nullable
        public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
            if (base == null) {
                throw new IllegalArgumentException("An introspected method requires a base object");
            }
            Object[] values = arguments == null ? new Object[0] : arguments;
            Object[] coerced = ELOverloads.coerce(context, method.getArguments(), values);
            if (coerced == null) {
                throw new ELException("The arguments do not match the method '" + method.getName() + "'");
            }
            return method.invoke(base, coerced);
        }

        @Override
        public String identity() {
            return method.getDeclaringType().getName() + '#' + method.getName()
                + java.util.Arrays.toString(Argument.toClassArray(method.getArguments()));
        }
    }

    /**
     * The introspection of a class as the resolver reads it.
     *
     * @param introspection The introspection, {@code null} when the class has none
     * @param properties    The properties by name
     * @param methods       The methods by name
     */
    private record Introspected(@Nullable BeanIntrospection<Object> introspection,
                                Map<String, BeanProperty<Object, Object>> properties,
                                Map<String, BeanMethod<Object, Object>[]> methods) {

        private static final Introspected NONE = new Introspected(null, Map.of(), Map.of());

        @SuppressWarnings("unchecked")
        static Introspected of(@Nullable BeanIntrospection<Object> introspection) {
            if (introspection == null) {
                return NONE;
            }
            Map<String, BeanProperty<Object, Object>> properties = new HashMap<>();
            for (BeanProperty<Object, Object> property : introspection.getBeanProperties()) {
                properties.put(property.getName(), property);
            }
            Map<String, List<BeanMethod<Object, Object>>> byName = new HashMap<>();
            for (BeanMethod<Object, Object> method : introspection.getBeanMethods()) {
                byName.computeIfAbsent(method.getName(), name -> new ArrayList<>(1)).add(method);
            }
            Map<String, BeanMethod<Object, Object>[]> methods = new HashMap<>();
            byName.forEach((name, list) -> methods.put(name, list.toArray(new BeanMethod[0])));
            return new Introspected(introspection, properties, methods);
        }
    }
}
