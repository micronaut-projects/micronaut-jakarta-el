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

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanMethod;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.core.type.Argument;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.PropertyNotWritableException;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
public final class IntrospectionELResolver extends ELResolver {

    private final BeanIntrospector introspector;
    private final boolean readOnly;

    /**
     * What the resolver needs of a class, read once: the introspection, its properties by name and its methods
     * by name. {@code BeanIntrospector.findIntrospection} loads the introspection through an {@code Optional}
     * on every call, and the methods of a name are otherwise filtered from all the methods on every invocation.
     */
    private final Map<Class<?>, Introspected> introspected = new ConcurrentHashMap<>();

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
        if (base == null || !(method instanceof String name)) {
            return null;
        }
        BeanMethod<Object, Object>[] named = introspected(base.getClass()).methods().get(name);
        if (named == null) {
            return null;
        }
        Object[] arguments = params == null ? new Object[0] : params;
        if (paramTypes != null) {
            for (BeanMethod<Object, Object> candidate : named) {
                if (sameTypes(candidate.getArguments(), paramTypes)) {
                    context.setPropertyResolved(base, method);
                    return candidate.invoke(base, coerceRequired(context, candidate.getArguments(), arguments));
                }
            }
            return null;
        }
        // Coercing the arguments is part of selecting the overload, so it happens before the resolver commits:
        // an overload the arguments do not fit is skipped, and when none fits the resolver declines and the
        // standard resolvers get their chance.
        for (BeanMethod<Object, Object> candidate : named.length == 1 ? List.of(named[0]) : candidates(named, arguments)) {
            Object[] coerced = coerce(context, candidate.getArguments(), arguments);
            if (coerced != null) {
                context.setPropertyResolved(base, method);
                return candidate.invoke(base, coerced);
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?> getCommonPropertyType(ELContext context, @Nullable Object base) {
        return base == null ? null : Object.class;
    }

    /**
     * The overloads of the given name that can take the arguments, in the order the section 1.6 of the
     * specification prefers them: a fixed arity overload whose parameters accept the arguments as they are,
     * then the other fixed arity overloads of the same arity, then the variable arity ones.
     */
    private static List<BeanMethod<Object, Object>> candidates(BeanMethod<Object, Object>[] named, Object[] arguments) {
        List<BeanMethod<Object, Object>> exact = new ArrayList<>(2);
        List<BeanMethod<Object, Object>> fixedArity = new ArrayList<>(2);
        List<BeanMethod<Object, Object>> variableArity = new ArrayList<>(2);
        for (BeanMethod<Object, Object> beanMethod : named) {
            Argument<?>[] parameters = beanMethod.getArguments();
            if (parameters.length == arguments.length) {
                (accepts(parameters, arguments) ? exact : fixedArity).add(beanMethod);
            } else if (isVariableArity(parameters) && arguments.length >= parameters.length - 1) {
                variableArity.add(beanMethod);
            }
        }
        if (!exact.isEmpty()) {
            return mostSpecific(exact);
        }
        fixedArity.addAll(variableArity);
        return fixedArity;
    }

    /**
     * Returns the uniquely most specific method from the candidates that already accept every argument, or no
     * method when the candidates are ambiguous. Returning no method lets the reflective resolver report the
     * ambiguity according to the EL method-selection rules.
     */
    private static List<BeanMethod<Object, Object>> mostSpecific(List<BeanMethod<Object, Object>> candidates) {
        BeanMethod<Object, Object> result = null;
        for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            BeanMethod<Object, Object> candidate = candidates.get(candidateIndex);
            boolean mostSpecific = true;
            for (int otherIndex = 0; otherIndex < candidates.size(); otherIndex++) {
                if (candidateIndex != otherIndex && !moreSpecific(candidate.getArguments(), candidates.get(otherIndex).getArguments())) {
                    mostSpecific = false;
                    break;
                }
            }
            if (!mostSpecific) {
                continue;
            }
            if (result != null) {
                return List.of();
            }
            result = candidate;
        }
        return result == null ? List.of() : List.of(result);
    }

    private static boolean moreSpecific(Argument<?>[] first, Argument<?>[] second) {
        for (int i = 0; i < first.length; i++) {
            if (!second[i].getWrapperType().isAssignableFrom(first[i].getWrapperType())) {
                return false;
            }
        }
        return true;
    }

    private static boolean accepts(Argument<?>[] parameters, Object[] arguments) {
        for (int i = 0; i < parameters.length; i++) {
            Object argument = arguments[i];
            Class<?> type = parameters[i].getWrapperType();
            if (argument == null ? parameters[i].getType().isPrimitive() : !type.isInstance(argument)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameTypes(Argument<?>[] parameters, Class<?>[] paramTypes) {
        if (parameters.length != paramTypes.length) {
            return false;
        }
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getWrapperType() != ReflectionUtils.getWrapperType(paramTypes[i])) {
                return false;
            }
        }
        return true;
    }

    private static Object[] coerceRequired(ELContext context, Argument<?>[] parameters, Object[] arguments) {
        Object[] coerced = coerce(context, parameters, arguments);
        if (coerced == null) {
            throw new ELException("The method expects " + parameters.length + " argument(s) but "
                + arguments.length + " were provided");
        }
        return coerced;
    }

    /**
     * A {@link BeanMethod} does not carry the variable arity flag of the method it dispatches to, so a trailing
     * array parameter is treated as variable arity. A fixed arity array parameter is still served: an array
     * given directly is passed through rather than wrapped.
     */
    private static boolean isVariableArity(Argument<?>[] parameters) {
        return parameters.length > 0 && parameters[parameters.length - 1].getType().isArray();
    }

    /**
     * Coerces the arguments to the parameters, packing the trailing ones into the array of a variable arity
     * method.
     *
     * @return The coerced arguments, or {@code null} when the arguments do not fit the parameters, so that the
     * overload is not selected
     */
    private static Object @Nullable [] coerce(ELContext context, Argument<?>[] parameters, Object[] arguments) {
        int fixed = parameters.length;
        Class<?> componentType = null;
        if (isVariableArity(parameters)) {
            Class<?> last = parameters[parameters.length - 1].getType();
            boolean arrayGivenDirectly = arguments.length == parameters.length
                && last.isInstance(arguments[arguments.length - 1]);
            if (!arrayGivenDirectly) {
                componentType = last.getComponentType();
                fixed = parameters.length - 1;
            }
        }
        if (componentType == null ? arguments.length != parameters.length : arguments.length < fixed) {
            return null;
        }
        try {
            Object[] coerced = new Object[parameters.length];
            for (int i = 0; i < fixed; i++) {
                coerced[i] = ELSupport.coerceToType(context, arguments[i], parameters[i].getType());
            }
            if (componentType != null) {
                Object variadic = Array.newInstance(componentType, arguments.length - fixed);
                for (int i = fixed; i < arguments.length; i++) {
                    Array.set(variadic, i - fixed, ELSupport.coerceToType(context, arguments[i], componentType));
                }
                coerced[parameters.length - 1] = variadic;
            }
            return coerced;
        } catch (ELException e) {
            // this overload does not accept these arguments
            return null;
        }
    }

    @Nullable
    private BeanProperty<Object, Object> findProperty(@Nullable Object base, @Nullable Object property) {
        if (base == null || !(property instanceof String name)) {
            return null;
        }
        return introspected(base.getClass()).properties().get(name);
    }

    private Introspected introspected(Class<?> type) {
        Introspected entry = introspected.get(type);
        if (entry == null) {
            entry = Introspected.of(findIntrospection(type));
            introspected.put(type, entry);
        }
        return entry;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private BeanIntrospection<Object> findIntrospection(Class<?> type) {
        return introspector.findIntrospection((Class<Object>) type).orElse(null);
    }

    private boolean isReadOnly(BeanProperty<Object, Object> beanProperty) {
        return readOnly || beanProperty.isReadOnly();
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
