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
package io.micronaut.el.interpreter.reflection;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.type.Argument;
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.runtime.ELMethods;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodNotFoundException;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resolves interpreter methods through the public Java reflection API.
 *
 * <p>This executor is registered as a service and is intentionally kept in a separate artifact. Applications
 * that need arbitrary Java method execution can add this module; applications that provide generated or
 * otherwise direct executors can omit it.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
public final class ReflectiveELMethodExecutor implements ELMethodExecutor {

    private static final String CONSTRUCTOR = "<init>";

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public boolean isReflective() {
        return true;
    }

    @Override
    @Nullable
    public ELMethod resolve(ELContext context,
                            @Nullable Object base,
                            @Nullable Object method,
                            Argument<?> @Nullable [] argumentTypes,
                            Object @Nullable [] arguments) {
        if (base == null || method == null) {
            return null;
        }
        String name = method.toString();
        if (base instanceof ELClass elClass) {
            if (CONSTRUCTOR.equals(name)) {
                Constructor<?> constructor = findConstructor(elClass.getKlass(), argumentTypes, arguments);
                return constructor == null ? null : new ConstructorMethod(constructor);
            }
            Method resolved = ReflectiveELMethods.findMethodOrNull(elClass.getKlass(), name,
                argumentTypes == null ? null : Argument.toClassArray(argumentTypes), arguments, true);
            return resolved == null ? null : new ReflectiveMethod(ReflectiveELMethods.accessible(resolved), true);
        }
        Method resolved = ReflectiveELMethods.findMethodOrNull(base.getClass(), name,
            argumentTypes == null ? null : Argument.toClassArray(argumentTypes), arguments, false);
        return resolved == null ? null : new ReflectiveMethod(ReflectiveELMethods.accessible(resolved), false);
    }

    @Override
    @Nullable
    public ELMethod resolveFunction(ELContext context, String prefix, String localName) {
        FunctionMapper functionMapper = context.getFunctionMapper();
        if (functionMapper == null) {
            return null;
        }
        Method method = functionMapper.resolveFunction(prefix, localName);
        if (method == null) {
            return null;
        }
        return new ReflectiveMethod(ReflectiveELMethods.accessible(method), Modifier.isStatic(method.getModifiers()));
    }

    @Nullable
    private static Constructor<?> findConstructor(Class<?> type,
                                                  Argument<?> @Nullable [] argumentTypes,
                                                  Object @Nullable [] arguments) {
        Constructor<?>[] constructors = type.getConstructors();
        List<ELMethods.Candidate<Constructor<?>>> candidates = new ArrayList<>(constructors.length);
        for (Constructor<?> constructor : constructors) {
            candidates.add(new ELMethods.Candidate<>(constructor, constructor.getParameterTypes(),
                constructor.isVarArgs(), true, false));
        }
        try {
            // the selection of the section 1.6, the one every other executor uses: an assignable parameter
            // is not as good as a more specific assignable one, and a tie does not hide an exact match
            return ELMethods.select(type, CONSTRUCTOR, candidates,
                argumentTypes == null ? null : Argument.toClassArray(argumentTypes),
                arguments == null ? new Object[0] : arguments, true);
        } catch (MethodNotFoundException e) {
            // the type declares no constructor these arguments fit, which the caller reports
            return null;
        }
    }

    private static final class ReflectiveMethod implements ELMethod {
        private final Class<?> owner;
        private final String name;
        private final Class<?>[] parameterTypes;
        private final boolean staticMethod;
        private transient @Nullable Method method;

        private ReflectiveMethod(Method method, boolean staticMethod) {
            Method accessible = accessible(method);
            this.owner = accessible.getDeclaringClass();
            this.name = accessible.getName();
            this.parameterTypes = accessible.getParameterTypes().clone();
            this.staticMethod = staticMethod;
            this.method = accessible;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Argument<?> getReturnType() {
            return Argument.of(method().getReturnType());
        }

        @Override
        public Argument<?>[] getArguments() {
            return Arrays.stream(parameterTypes).map(Argument::of).toArray(Argument<?>[]::new);
        }

        @Override
        public boolean isVarArgs() {
            return method().isVarArgs();
        }

        @Override
        public java.lang.annotation.Annotation[] synthesizeAnnotations() {
            return method().getAnnotations();
        }

        @Override
        @Nullable
        public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
            Method resolved = method();
            return ReflectiveELMethods.invoke(context, resolved, staticMethod ? null : base, arguments);
        }

        @Override
        public String identity() {
            return io.micronaut.el.parser.ELNodes.functionIdentity(owner.getName(), name,
                Arrays.stream(parameterTypes).map(Class::getTypeName).toList());
        }

        private Method method() {
            Method resolved = method;
            if (resolved == null) {
                resolved = ReflectiveELMethods.findMethodOrNull(owner, name, parameterTypes, null, staticMethod);
                if (resolved == null) {
                    throw new ELException("Cannot restore the method '" + owner.getName() + '.' + name + "'");
                }
                method = accessible(resolved);
            }
            return resolved;
        }

        private static Method accessible(Method method) {
            Method accessible = ReflectiveELMethods.accessible(method);
            if (!Modifier.isPublic(accessible.getDeclaringClass().getModifiers())) {
                accessible.trySetAccessible();
            }
            return accessible;
        }
    }

    private static final class ConstructorMethod implements ELMethod {
        private final Class<?> owner;
        private final Class<?>[] parameterTypes;
        private final boolean varArgs;
        private transient @Nullable Constructor<?> constructor;

        private ConstructorMethod(Constructor<?> constructor) {
            this.owner = constructor.getDeclaringClass();
            this.parameterTypes = constructor.getParameterTypes().clone();
            this.varArgs = constructor.isVarArgs();
            this.constructor = constructor;
        }

        @Override
        public String getName() {
            return CONSTRUCTOR;
        }

        @Override
        public Argument<?> getReturnType() {
            return Argument.of(owner);
        }

        @Override
        public Argument<?>[] getArguments() {
            return Arrays.stream(parameterTypes).map(Argument::of).toArray(Argument<?>[]::new);
        }

        @Override
        public boolean isVarArgs() {
            return varArgs;
        }

        @Override
        @Nullable
        public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
            Object[] values = arguments == null ? new Object[0] : arguments;
            int fixed = varArgs ? parameterTypes.length - 1 : parameterTypes.length;
            if (varArgs ? values.length < fixed : values.length != fixed) {
                throw new IllegalArgumentException("The constructor of '" + owner.getName() + "' expects "
                    + (varArgs ? "at least " + fixed : fixed) + " argument(s) but " + values.length + " were provided");
            }
            // the coercion and the packing of the variable arity arguments every other invocation uses, which
            // passes a single array assignable to the variable arity parameter as that array
            Object[] parameters = ELMethods.coerceArguments(context, CONSTRUCTOR, parameterTypes, varArgs, values);
            try {
                return constructor().newInstance(parameters);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
                throw new ELException(e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw cause instanceof ELException elException ? elException : new ELException(cause);
            }
        }

        private Constructor<?> constructor() {
            Constructor<?> resolved = constructor;
            if (resolved == null) {
                try {
                    resolved = owner.getConstructor(parameterTypes);
                } catch (NoSuchMethodException e) {
                    throw new ELException("Cannot restore a constructor of '" + owner.getName() + "'", e);
                }
                constructor = resolved;
            }
            return resolved;
        }

        @Override
        public String identity() {
            return owner.getName() + "#<init>" + Arrays.toString(parameterTypes);
        }
    }
}
