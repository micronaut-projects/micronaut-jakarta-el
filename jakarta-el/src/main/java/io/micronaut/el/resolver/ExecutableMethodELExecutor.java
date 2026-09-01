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

import io.micronaut.context.BeanContext;
import io.micronaut.context.BeanDefinitionRegistry;
import io.micronaut.context.exceptions.BeanContextException;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.type.Argument;
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.runtime.ELArguments;
import io.micronaut.el.stream.ELOptional;
import io.micronaut.el.stream.ELStream;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.ProxyBeanDefinition;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.LambdaExpression;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * The {@link ELMethodExecutor} invoking a method of a bean through the executable metadata its bean definition
 * carries, the metadata Micronaut generates for every method annotated with
 * {@code io.micronaut.context.annotation.Executable}, directly or through an annotation that is itself
 * meta-annotated with it.
 *
 * <p>A bean introspection and an executable method are two independent descriptions of a type, and a great
 * many beans carry the second without the first: anything AOP-advised, anything a framework marks executable
 * for its own dispatch. This executor makes such a method reachable from an expression without the type also
 * having to be annotated {@code @Introspected}.</p>
 *
 * <p>The class is both an {@link ELMethodExecutor} and an {@link ELResolver} because the two execution paths of
 * this project reach a method differently: an expression parsed at runtime resolves it through the
 * {@link ELMethodExecutor} services, while an expression compiled at compilation time dispatches through the
 * resolver chain of its context, {@code ELResolution.invoke} calling {@code ELContext.getELResolver().invoke}.
 * One implementation answers both, so a method resolves the same way whichever path an expression took.</p>
 *
 * <p>The executor is registered in {@code META-INF/services/io.micronaut.el.ELMethodExecutor}, so it is
 * available to every expression parsed at runtime with no wiring at all. Registered that way it is constructed
 * with the no-argument constructor and has no bean context of its own, so it reads one from the
 * {@link ELContext} of each call, which an application registers once:</p>
 *
 * <pre>{@code
 * context.putContext(BeanDefinitionRegistry.class, beanContext);
 * }</pre>
 *
 * <p>{@code BeanContext.class} works as the key as well. A context that carries neither is not one this
 * executor can read, so it declines and the rest of the chain resolves the method as it did before. Reading the
 * registry from the context rather than from a static holder is what lets an application that runs more than
 * one bean context resolve a method in the context the expression is evaluated for; the
 * {@link #ExecutableMethodELExecutor(BeanDefinitionRegistry)} constructor binds one directly, for the resolver
 * chain and for an application registering the executor programmatically.</p>
 *
 * <p>The executor only invokes: the properties stay with the rest of the chain. A base whose class is not a
 * bean of the context has no definition, so the executor declines and the reflective one gets its chance. The
 * runtime class of an intercepted instance is the generated proxy, whose definition is a
 * {@link ProxyBeanDefinition}; the executor reads the executable methods of the target definition, so the
 * method that is found is the one the author wrote.</p>
 *
 * <p>Like {@link IntrospectionELResolver}, the executor reads compiled metadata rather than reflection, so it
 * works under a GraalVM native image without any registration.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.1
 */
@Experimental
public final class ExecutableMethodELExecutor extends ELResolver implements ELMethodExecutor {

    /**
     * After {@link IntrospectionELResolver}, whose introspection is the more precise description where it
     * exists, and before the reflective executor, which is the last resort.
     */
    private static final int ORDER = 200;

    private static final String CONSTRUCTOR = "<init>";

    /**
     * The executable methods by class of the registry this executor was constructed with, or {@code null} when
     * it was constructed as a service and reads the registry of each call from the context instead.
     */
    @Nullable
    private final Executables bound;

    /**
     * The executable methods by class of every registry this executor has been asked about, for the service
     * form, which sees a registry per call rather than one for its lifetime.
     *
     * <p>The keys are weak, and an {@link Executables} holds its registry weakly in turn, so a bean context
     * that was closed and dropped stays collectable however many expressions were evaluated against it.</p>
     */
    private final Map<BeanDefinitionRegistry, Executables> contextual =
        Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Creates the executor reading, on every call, the registry registered in the {@link ELContext} the
     * expression is evaluated with, under the key {@code BeanDefinitionRegistry.class} or
     * {@code BeanContext.class}.
     *
     * <p>This is the constructor the service loader uses. An expression evaluated with a context that carries
     * no registry resolves the way it did before this executor existed.</p>
     */
    public ExecutableMethodELExecutor() {
        this.bound = null;
    }

    /**
     * Creates the executor reading the definitions of a registry.
     *
     * <p>A {@code io.micronaut.context.BeanContext} is one, and is what an application usually has. The
     * narrower type is what this executor actually needs: it reads the executable metadata of a definition and
     * invokes the method on the instance the expression produced, so it never looks a bean up, creates one, or
     * touches the lifecycle of the context.</p>
     *
     * @param registry The registry whose definitions carry the executable methods
     */
    public ExecutableMethodELExecutor(BeanDefinitionRegistry registry) {
        this.bound = Executables.retaining(Objects.requireNonNull(registry,
            "The bean definition registry cannot be null"));
    }

    @Override
    public int getOrder() {
        return ORDER;
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
        if (base == null || declined(base) || !(method instanceof String name) || CONSTRUCTOR.equals(name)) {
            return null;
        }
        Executables executables = executables(context);
        if (executables == null) {
            return null;
        }
        List<ExecutableMethod<Object, Object>> named = executables.get(base.getClass()).get(name);
        if (named == null) {
            return null;
        }
        Object[] values = arguments == null ? new Object[0] : arguments;
        ExecutableMethod<Object, Object> selected = argumentTypes == null
            ? ELOverloads.select(context, named, ExecutableMethod::getArguments, values)
            : ELOverloads.declaring(context, named, ExecutableMethod::getArguments, argumentTypes, values);
        return selected == null ? null : new ExecutableELMethod(selected);
    }

    @Override
    @Nullable
    public Object getValue(ELContext context, @Nullable Object base, @Nullable Object property) {
        return null;
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext context, @Nullable Object base, @Nullable Object property) {
        return null;
    }

    @Override
    public void setValue(ELContext context, @Nullable Object base, @Nullable Object property, @Nullable Object value) {
        // not a property resolver
    }

    @Override
    public boolean isReadOnly(ELContext context, @Nullable Object base, @Nullable Object property) {
        return false;
    }

    @Override
    @Nullable
    public Class<?> getCommonPropertyType(ELContext context, @Nullable Object base) {
        return null;
    }

    /**
     * The bases the specification, or this module, gives a resolver of their own for invocation: a static
     * reference, the streams and the optionals, and a lambda expression.
     *
     * <p>Read by {@link ELMethodDiagnostics} as well, so that a base this executor was never going to answer
     * for is not reported as one whose bean definition was missing something.</p>
     */
    static boolean declined(Object base) {
        return base instanceof ELClass
            || base instanceof ELStream<?>
            || base instanceof ELOptional<?>
            || base instanceof Optional<?>
            || base instanceof LambdaExpression;
    }

    /**
     * The registry an expression evaluated with the given context reads its executable methods from: the one
     * this executor was constructed with, or the one the context carries.
     */
    @Nullable
    private Executables executables(ELContext context) {
        if (bound != null) {
            return bound;
        }
        BeanDefinitionRegistry registry = registryOf(context);
        return registry == null ? null : contextual.computeIfAbsent(registry, Executables::weak);
    }

    /**
     * The registry registered in the context, under the type this executor needs or under the wider
     * {@code BeanContext} an application is more likely to have at hand.
     *
     * <p>{@link ELMethodDiagnostics} reads it through this method too, so that what a failed resolution
     * reports about the context is what this executor actually saw of it.</p>
     */
    @Nullable
    static BeanDefinitionRegistry registryOf(ELContext context) {
        Object registered = context.getContext(BeanDefinitionRegistry.class);
        if (registered instanceof BeanDefinitionRegistry registry) {
            return registry;
        }
        Object beanContext = context.getContext(BeanContext.class);
        return beanContext instanceof BeanDefinitionRegistry registry ? registry : null;
    }

    /**
     * The definition a class resolves to, for {@link ELMethodDiagnostics}: the same lookup this executor makes,
     * proxies resolved to their target, so that a failed resolution reports the executable methods that were
     * actually searched.
     *
     * @param registry The registry to read
     * @param type     The runtime class of the base object
     * @return The definition, or {@code null} when the class is not a bean of the registry
     */
    @Nullable
    static BeanDefinition<Object> definitionOf(BeanDefinitionRegistry registry, Class<?> type) {
        return Executables.definitionOf(registry, type);
    }

    /**
     * A method invoked through the executable metadata of a bean definition.
     */
    private static final class ExecutableELMethod implements ELMethod {

        private final ExecutableMethod<Object, Object> method;

        private ExecutableELMethod(ExecutableMethod<Object, Object> method) {
            this.method = method;
        }

        @Override
        public String getName() {
            return method.getMethodName();
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
                throw new IllegalArgumentException("An executable method requires a base object");
            }
            Object[] values = arguments == null ? new Object[0] : arguments;
            Object[] coerced = ELOverloads.coerce(context, method.getArguments(), values);
            if (coerced == null) {
                throw new ELException("The arguments do not match the method '" + method.getMethodName() + "'");
            }
            return method.invoke(base, coerced);
        }

        @Override
        public String identity() {
            return method.getDeclaringType().getName() + '#' + method.getMethodName()
                + Arrays.toString(Argument.toClassArray(method.getArguments()));
        }
    }

    /**
     * The executable methods of a registry by class and then by name, read once per class: resolving the bean
     * definition of a class walks the definitions of the context, and the methods of a name are otherwise
     * filtered from all the executable methods on every invocation. A class that is not a bean caches an empty
     * map, so the walk is not repeated for every base object the chain offers.
     *
     * <p>How the registry is held depends on where these came from. The executor constructed with a registry
     * holds it, since nothing else in the chain does and an application that handed one over expects it to
     * stay usable. The service form keeps one of these per registry in a map whose keys are weak, and a value
     * that referred to its own key strongly would defeat it, so it holds the registry weakly instead; a
     * registry that has been collected has no definitions left to read, and every class then maps to nothing.
     * </p>
     */
    private static final class Executables extends ClassValue<Map<String, List<ExecutableMethod<Object, Object>>>> {

        @Nullable
        private final BeanDefinitionRegistry retained;
        private final WeakReference<BeanDefinitionRegistry> registry;

        private Executables(@Nullable BeanDefinitionRegistry retained, BeanDefinitionRegistry registry) {
            this.retained = retained;
            this.registry = new WeakReference<>(registry);
        }

        /**
         * The executable methods of a registry this executor was constructed with, and keeps alive.
         */
        static Executables retaining(BeanDefinitionRegistry registry) {
            return new Executables(registry, registry);
        }

        /**
         * The executable methods of a registry a context handed over, which stays collectable.
         */
        static Executables weak(BeanDefinitionRegistry registry) {
            return new Executables(null, registry);
        }

        @Override
        protected Map<String, List<ExecutableMethod<Object, Object>>> computeValue(Class<?> type) {
            BeanDefinitionRegistry beanDefinitions = retained == null ? registry.get() : retained;
            if (beanDefinitions == null) {
                return Map.of();
            }
            BeanDefinition<Object> definition = definitionOf(beanDefinitions, type);
            if (definition == null) {
                return Map.of();
            }
            Map<String, List<ExecutableMethod<Object, Object>>> byName = new HashMap<>();
            for (ExecutableMethod<Object, ?> method : definition.getExecutableMethods()) {
                @SuppressWarnings("unchecked")
                ExecutableMethod<Object, Object> executable = (ExecutableMethod<Object, Object>) method;
                byName.computeIfAbsent(executable.getMethodName(), name -> new ArrayList<>(1)).add(executable);
            }
            Map<String, List<ExecutableMethod<Object, Object>>> methods = new HashMap<>(byName.size());
            byName.forEach((name, overloads) -> methods.put(name, List.copyOf(overloads)));
            return methods;
        }

        /**
         * The definition of a class, resolved through to the target definition when the class is a generated
         * proxy, so that the executable methods of the class the author wrote are the ones that are read.
         */
        @Nullable
        static BeanDefinition<Object> definitionOf(BeanDefinitionRegistry registry, Class<?> type) {
            @SuppressWarnings("unchecked")
            Class<Object> beanType = (Class<Object>) type;
            BeanDefinition<Object> definition = findDefinition(registry, beanType);
            if (definition instanceof ProxyBeanDefinition<Object> proxy) {
                BeanDefinition<Object> target = findTarget(registry, definition);
                return target == null ? findDefinition(registry, proxy.getTargetType()) : target;
            }
            return definition;
        }

        @Nullable
        private static BeanDefinition<Object> findDefinition(BeanDefinitionRegistry registry, Class<Object> beanType) {
            try {
                return registry.findBeanDefinition(beanType).orElse(null);
            } catch (BeanContextException e) {
                // an ambiguous or unloadable definition is not one this executor can read
                return null;
            }
        }

        @Nullable
        private static BeanDefinition<Object> findTarget(BeanDefinitionRegistry registry,
                                                         BeanDefinition<Object> proxy) {
            try {
                return registry.findProxyTargetBeanDefinition(proxy).orElse(null);
            } catch (BeanContextException e) {
                return null;
            }
        }
    }
}
