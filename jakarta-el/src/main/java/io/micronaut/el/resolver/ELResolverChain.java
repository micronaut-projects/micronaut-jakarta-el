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
import jakarta.el.CompositeELResolver;
import jakarta.el.ELContext;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link CompositeELResolver} that only consults, for an operation a resolver does not have to implement,
 * the resolvers that implement it.
 *
 * <p>{@link ELResolver#convertToType} and {@link ELResolver#invoke} have a default implementation that resolves
 * nothing, and none of the resolvers of the specification overrides the former. The composite of the
 * specification still asks every resolver on every coercion, which is the single largest cost of evaluating a
 * compiled expression: the result of every value expression is coerced to its expected type. This composite
 * records, when a resolver is added, whether its class overrides each of the two methods, and iterates only the
 * resolvers that do. The semantics are the ones of {@link CompositeELResolver}: the resolvers are consulted in
 * order until one sets {@code propertyResolved}.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public final class ELResolverChain extends CompositeELResolver {

    private static final int CONVERTS = 1;
    private static final int INVOKES = 2;

    /**
     * Which of the two methods the resolvers of the specification and of this module leave to
     * {@link ELResolver}, so that a chain does not offer them a call they would only decline.
     *
     * <p>This is declared rather than read from the class: reading it would mean reflecting on every
     * resolver of every chain, in a module whose runtime does not otherwise reflect. A resolver that is not
     * named here is offered both, which costs a call it may decline and is what a resolver an application
     * contributes gets.</p>
     */
    private static final Map<Class<?>, Integer> DECLINES = Map.ofEntries(
        Map.entry(CommonELResolver.class, CONVERTS),
        Map.entry(IntrospectionELResolver.class, CONVERTS),
        Map.entry(StreamELResolver.class, CONVERTS),
        Map.entry(ExecutableMethodELExecutor.class, CONVERTS),
        Map.entry(StaticFieldELResolver.class, CONVERTS),
        Map.entry(MapELResolver.class, CONVERTS | INVOKES),
        Map.entry(ResourceBundleELResolver.class, CONVERTS | INVOKES),
        Map.entry(ListELResolver.class, CONVERTS | INVOKES),
        Map.entry(ArrayELResolver.class, CONVERTS | INVOKES),
        Map.entry(RecordELResolver.class, CONVERTS | INVOKES),
        Map.entry(BeanELResolver.class, CONVERTS)
    );

    /**
     * The two methods minus the ones the resolver declines. {@code OptionalELResolver} declines neither,
     * which is why it is not named above.
     */

    /**
     * The resolvers of the specification, and of this module, that resolve nothing without a base object: the
     * identifiers, resolved with a null base, are not offered to them.
     */
    private static final Set<Class<?>> BASE_REQUIRED = Set.of(
        CommonELResolver.class, IntrospectionELResolver.class, StreamELResolver.class,
        ExecutableMethodELExecutor.class,
        StaticFieldELResolver.class, MapELResolver.class, ResourceBundleELResolver.class, ListELResolver.class,
        ArrayELResolver.class, RecordELResolver.class, OptionalELResolver.class, BeanELResolver.class);

    private ELResolver[] resolvers = new ELResolver[0];
    private ELResolver[] identifiers = new ELResolver[0];
    private ELResolver[] converters = new ELResolver[0];
    private ELResolver[] invokers = new ELResolver[0];

    /**
     * Creates a chain of the given resolvers, in order.
     *
     * @param resolvers The resolvers
     */
    public ELResolverChain(ELResolver... resolvers) {
        for (ELResolver resolver : resolvers) {
            add(resolver);
        }
    }

    /**
     * Creates a chain of the given resolvers, in order.
     *
     * @param resolvers The resolvers
     */
    public ELResolverChain(List<? extends ELResolver> resolvers) {
        for (ELResolver resolver : resolvers) {
            add(resolver);
        }
    }

    /**
     * Whether the chain treats the resolver as overriding {@code convertToType}, which its test holds to what
     * the class actually does.
     *
     * @param type The class of resolver
     * @return Whether it is offered a conversion
     */
    static boolean overridesConvertToType(Class<?> type) {
        return (overridesOf(type) & CONVERTS) != 0;
    }

    /**
     * Whether the chain treats the resolver as overriding {@code invoke}.
     *
     * @param type The class of resolver
     * @return Whether it is offered an invocation
     */
    static boolean overridesInvoke(Class<?> type) {
        return (overridesOf(type) & INVOKES) != 0;
    }

    private static int overridesOf(Class<?> type) {
        return (CONVERTS | INVOKES) & ~DECLINES.getOrDefault(type, 0);
    }

    @Override
    public void add(ELResolver elResolver) {
        if (elResolver instanceof ELResolverChain chain) {
            // flattened: one level of iteration for every operation
            for (ELResolver nested : chain.resolvers) {
                add(nested);
            }
            return;
        }
        super.add(elResolver);
        resolvers = concat(resolvers, new ELResolver[] {elResolver});
        if (!BASE_REQUIRED.contains(elResolver.getClass())) {
            identifiers = concat(identifiers, new ELResolver[] {elResolver});
        }
        int overrides = overridesOf(elResolver.getClass());
        if ((overrides & CONVERTS) != 0) {
            converters = concat(converters, new ELResolver[] {elResolver});
        }
        if ((overrides & INVOKES) != 0) {
            invokers = concat(invokers, new ELResolver[] {elResolver});
        }
    }

    /**
     * @return Whether any of the resolvers implements {@link ELResolver#convertToType}
     */
    public boolean converts() {
        return converters.length > 0;
    }

    @Override
    @Nullable
    public Object getValue(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base != null) {
            return super.getValue(context, base, property);
        }
        context.setPropertyResolved(false);
        for (ELResolver resolver : identifiers) {
            Object value = resolver.getValue(context, null, property);
            if (context.isPropertyResolved()) {
                return value;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public <T> T convertToType(ELContext context, @Nullable Object obj, @Nullable Class<T> targetType) {
        context.setPropertyResolved(false);
        for (ELResolver converter : converters) {
            if (converter instanceof OptionalELResolver && !(obj instanceof Optional)) {
                // the resolver of the specification only converts an Optional, the one converter of the
                // standard chain
                continue;
            }
            T value = converter.convertToType(context, obj, targetType);
            if (context.isPropertyResolved()) {
                return value;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Object invoke(ELContext context, @Nullable Object base, @Nullable Object method, Class<?> @Nullable [] paramTypes, @Nullable Object[] params) {
        context.setPropertyResolved(false);
        for (ELResolver invoker : invokers) {
            Object value = invoker.invoke(context, base, method, paramTypes, params);
            if (context.isPropertyResolved()) {
                return value;
            }
        }
        return null;
    }

    /**
     * @return Whether the class of the resolver, or a superclass below {@link ELResolver}, declares the method
     */
    private static ELResolver[] concat(ELResolver[] first, ELResolver[] second) {
        List<ELResolver> all = new ArrayList<>(first.length + second.length);
        all.addAll(List.of(first));
        all.addAll(List.of(second));
        return all.toArray(new ELResolver[0]);
    }
}
