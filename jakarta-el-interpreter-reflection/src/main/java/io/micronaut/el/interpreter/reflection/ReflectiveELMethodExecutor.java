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
import io.micronaut.el.runtime.ELSupport;
import io.micronaut.el.runtime.ELMethods;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodNotFoundException;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
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
            return resolved == null ? null : new ReflectiveMethod(ReflectiveELMethods.accessible(resolved), true, false);
        }
        Method resolved = ReflectiveELMethods.findMethodOrNull(base.getClass(), name,
            argumentTypes == null ? null : Argument.toClassArray(argumentTypes), arguments, false);
        return resolved == null ? null : new ReflectiveMethod(ReflectiveELMethods.accessible(resolved), false, false);
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
        return new ReflectiveMethod(ReflectiveELMethods.accessible(method), Modifier.isStatic(method.getModifiers()), true);
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

    private static boolean sameTypes(Class<?>[] declared, Argument<?>[] provided) {
        if (declared.length != provided.length) {
            return false;
        }
        for (int i = 0; i < declared.length; i++) {
            if (wrap(declared[i]) != provided[i].getWrapperType()) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        return switch (type.getName()) {
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "char" -> Character.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            case "void" -> Void.class;
            default -> type;
        };
    }

    private static final class ReflectiveMethod implements ELMethod {
        private final Class<?> owner;
        private final String name;
        private final Class<?>[] parameterTypes;
        private final boolean staticMethod;
        private final boolean directArrayVarargs;
        private transient @Nullable Method method;

        private ReflectiveMethod(Method method, boolean staticMethod, boolean directArrayVarargs) {
            Method accessible = accessible(method);
            this.owner = accessible.getDeclaringClass();
            this.name = accessible.getName();
            this.parameterTypes = accessible.getParameterTypes().clone();
            this.staticMethod = staticMethod;
            this.directArrayVarargs = directArrayVarargs;
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
            return directArrayVarargs
                ? invokeFunction(context, resolved, staticMethod ? null : base, arguments)
                : ReflectiveELMethods.invoke(context, resolved, staticMethod ? null : base, arguments);
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

        @Nullable
        private static Object invokeFunction(ELContext context,
                                              Method method,
                                              @Nullable Object base,
                                              Object @Nullable [] values) {
            if (!method.isVarArgs()) {
                return ReflectiveELMethods.invoke(context, method, base, values);
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] arguments = values == null ? new Object[0] : values;
            int fixed = parameterTypes.length - 1;
            if (arguments.length < fixed) {
                throw new IllegalArgumentException("The method '" + method.getName() + "' expects at least "
                    + fixed + " argument(s) but " + arguments.length + " were provided");
            }
            Object[] parameters = new Object[parameterTypes.length];
            for (int i = 0; i < fixed; i++) {
                parameters[i] = ELSupport.coerceToType(context, arguments[i], parameterTypes[i]);
            }
            if (arguments.length == parameterTypes.length && arguments[fixed] != null
                && parameterTypes[fixed].isInstance(arguments[fixed])) {
                parameters[fixed] = arguments[fixed];
            } else {
                Class<?> componentType = parameterTypes[fixed].getComponentType();
                Object varargs = Array.newInstance(componentType, arguments.length - fixed);
                for (int i = fixed; i < arguments.length; i++) {
                    Array.set(varargs, i - fixed, ELSupport.coerceToType(context, arguments[i], componentType));
                }
                parameters[fixed] = varargs;
            }
            try {
                return method.invoke(base, parameters);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new ELException(e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw cause instanceof ELException elException ? elException : new ELException(cause);
            }
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
        // the arity check, the packing of the variable arity arguments and the coercion of each are one
        // invocation, and splitting them would hide what the constructor is handed
        @SuppressWarnings("java:S3776")
        public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
            Object[] values = arguments == null ? new Object[0] : arguments;
            int fixed = varArgs ? parameterTypes.length - 1 : parameterTypes.length;
            if (varArgs ? values.length < fixed : values.length != fixed) {
                throw new IllegalArgumentException("The constructor of '" + owner.getName() + "' expects "
                    + (varArgs ? "at least " + fixed : fixed) + " argument(s) but " + values.length + " were provided");
            }
            Object[] parameters = new Object[parameterTypes.length];
            for (int i = 0; i < fixed; i++) {
                parameters[i] = io.micronaut.el.runtime.ELSupport.coerceToType(context, values[i], parameterTypes[i]);
            }
            if (varArgs) {
                if (values.length == parameterTypes.length && values[fixed] != null
                    && parameterTypes[fixed].isInstance(values[fixed])) {
                    parameters[fixed] = values[fixed];
                } else {
                    Class<?> componentType = parameterTypes[fixed].getComponentType();
                    Object varargs = java.lang.reflect.Array.newInstance(componentType, values.length - fixed);
                    for (int i = fixed; i < values.length; i++) {
                        io.micronaut.el.runtime.ELArray.set(varargs, i - fixed,
                            io.micronaut.el.runtime.ELSupport.coerceToType(context, values[i], componentType));
                    }
                    parameters[fixed] = varargs;
                }
            }
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
