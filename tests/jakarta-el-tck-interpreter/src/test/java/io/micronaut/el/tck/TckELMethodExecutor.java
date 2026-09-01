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
package io.micronaut.el.tck;

import com.sun.ts.tests.el.common.util.MethodsBean;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.type.Argument;
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.function.Predicate;

/**
 * Direct method dispatch for the Java methods exercised by the TCK.
 */
@SuppressWarnings("deprecation")
public final class TckELMethodExecutor implements ELMethodExecutor {

    private static final List<DirectMethod> VECTOR_ADD = List.of(
        method("add", boolean.class, new Class<?>[]{Object.class}, false,
            (base, arguments) -> vector(base).add(arguments[0])),
        method("add", void.class, new Class<?>[]{int.class, Object.class}, false,
            (base, arguments) -> {
                vector(base).add((Integer) arguments[0], arguments[1]);
                return null;
            })
    );
    private static final List<DirectMethod> METHODS_BEAN = List.of(
        method("targetA", String.class, new Class<?>[]{CharSequence.class}, false,
            (base, arguments) -> bean(base).targetA((CharSequence) arguments[0])),
        method("targetA", String.class, new Class<?>[]{String.class}, false,
            (base, arguments) -> bean(base).targetA((String) arguments[0])),
        method("targetA", String.class, new Class<?>[]{Long.class}, false,
            (base, arguments) -> bean(base).targetA((Long) arguments[0])),
        method("targetB", String.class, new Class<?>[]{CharSequence.class}, false,
            (base, arguments) -> bean(base).targetB((CharSequence) arguments[0])),
        method("targetB", String.class, new Class<?>[]{Long.class}, false,
            (base, arguments) -> bean(base).targetB((Long) arguments[0])),
        method("targetC", String.class, new Class<?>[]{CharSequence.class, CharSequence.class}, false,
            (base, arguments) -> bean(base).targetC((CharSequence) arguments[0], (CharSequence) arguments[1])),
        method("targetC", String.class, new Class<?>[]{String.class, String[].class}, true,
            (base, arguments) -> bean(base).targetC((String) arguments[0], (String[]) arguments[1])),
        method("targetD", String.class, new Class<?>[]{Long.class, Long.class}, false,
            (base, arguments) -> bean(base).targetD((Long) arguments[0], (Long) arguments[1])),
        method("targetD", String.class, new Class<?>[]{String.class, String[].class}, true,
            (base, arguments) -> bean(base).targetD((String) arguments[0], (String[]) arguments[1])),
        method("targetE", String.class, new Class<?>[]{Long.class, Long.class}, false,
            (base, arguments) -> bean(base).targetE((Long) arguments[0], (Long) arguments[1])),
        method("targetE", String.class, new Class<?>[]{String.class, String.class}, false,
            (base, arguments) -> bean(base).targetE((String) arguments[0], (String) arguments[1])),
        new DirectMethod("targetF", String.class, new Class<?>[]{String.class, Long.class}, false,
            new Annotation[]{DeprecatedLiteral.INSTANCE},
            (base, arguments) -> bean(base).targetF((String) arguments[0], (Long) arguments[1]))
    );

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public @Nullable ELMethod resolve(ELContext context,
                                      @Nullable Object base,
                                      @Nullable Object method,
                                      Argument<?> @Nullable [] argumentTypes,
                                      Object @Nullable [] arguments) {
        if (base == null || method == null) {
            return null;
        }
        String name = method.toString();
        if (base instanceof Vector<?> && name.equals("add")) {
            return select(context, VECTOR_ADD, argumentTypes, arguments);
        }
        if (base instanceof ArrayList<?> && name.equals("add")) {
            return method("add", boolean.class, new Class<?>[]{Object.class}, false,
                (target, values) -> arrayList(target).add(values[0]));
        }
        if (base instanceof Integer && name.equals("compareTo")) {
            return method("compareTo", int.class, new Class<?>[]{Integer.class}, false,
                (target, values) -> ((Integer) target).compareTo((Integer) values[0]));
        }
        if (base instanceof Long && name.equals("compareTo")) {
            boolean exact = arguments != null && arguments.length == 1 && arguments[0] instanceof Long;
            return method("compareTo", int.class, new Class<?>[]{exact ? Long.class : Object.class}, false,
                (target, values) -> ((Long) target).compareTo((Long) values[0]));
        }
        if (base instanceof String && name.equals("compareTo")) {
            boolean exact = arguments != null && arguments.length == 1 && arguments[0] instanceof String;
            return method("compareTo", int.class, new Class<?>[]{exact ? String.class : Object.class}, false,
                (target, values) -> ((String) target).compareTo((String) values[0]));
        }
        if (base instanceof String && name.equals("equals")) {
            return method("equals", boolean.class, new Class<?>[]{Object.class}, false,
                (target, values) -> target.equals(values[0]));
        }
        if (base instanceof MethodsBean) {
            return select(context, METHODS_BEAN.stream().filter(candidate -> candidate.getName().equals(name)).toList(),
                argumentTypes, arguments);
        }
        return null;
    }

    @Override
    public @Nullable ELMethod resolveFunction(ELContext context, String prefix, String localName) {
        if (prefix.equals("Int") && localName.equals("val")) {
            return method("valueOf", Integer.class, new Class<?>[]{String.class}, false,
                (base, arguments) -> Integer.valueOf((String) arguments[0]));
        }
        if (!prefix.isEmpty()) {
            return null;
        }
        return switch (localName) {
            case "testPrimitiveBooleanArray" -> method(localName, int.class,
                new Class<?>[]{boolean[].class}, false,
                (base, arguments) -> com.sun.ts.tests.el.spec.coercion.ELClientIT
                    .testPrimitiveBooleanArray((boolean[]) arguments[0]));
            case "testPredicateString" -> method(localName, String.class,
                new Class<?>[]{Predicate.class}, false,
                (base, arguments) -> com.sun.ts.tests.el.spec.coercion.ELClientIT
                    .testPredicateString(predicate(arguments[0])));
            case "testPredicateLong" -> method(localName, String.class,
                new Class<?>[]{Predicate.class}, false,
                (base, arguments) -> com.sun.ts.tests.el.spec.coercion.ELClientIT
                    .testPredicateLong(predicate(arguments[0])));
            default -> null;
        };
    }

    private static @Nullable DirectMethod select(ELContext context,
                                                  List<DirectMethod> candidates,
                                                  Argument<?> @Nullable [] argumentTypes,
                                                  Object @Nullable [] arguments) {
        DirectMethod selected = null;
        int selectedScore = Integer.MAX_VALUE;
        for (DirectMethod candidate : candidates) {
            int score = candidate.score(context, argumentTypes, arguments);
            if (score < selectedScore) {
                selected = candidate;
                selectedScore = score;
            } else if (score == selectedScore) {
                selected = null;
            }
        }
        return selected;
    }

    private static DirectMethod method(String name,
                                       Class<?> returnType,
                                       Class<?>[] parameterTypes,
                                       boolean varArgs,
                                       Invoker invoker) {
        return new DirectMethod(name, returnType, parameterTypes, varArgs, new Annotation[0], invoker);
    }

    @SuppressWarnings("unchecked")
    private static Vector<Object> vector(@Nullable Object base) {
        return (Vector<Object>) base;
    }

    @SuppressWarnings("unchecked")
    private static ArrayList<Object> arrayList(@Nullable Object base) {
        return (ArrayList<Object>) base;
    }

    private static MethodsBean bean(@Nullable Object base) {
        return (MethodsBean) base;
    }

    @SuppressWarnings("unchecked")
    private static <T> Predicate<T> predicate(Object value) {
        return (Predicate<T>) value;
    }

    @FunctionalInterface
    private interface Invoker extends Serializable {
        @Nullable Object invoke(@Nullable Object base, Object[] arguments);
    }

    private static final class DirectMethod implements ELMethod {
        private final String name;
        private final Class<?> returnType;
        private final Class<?>[] parameterTypes;
        private final boolean varArgs;
        private final Annotation[] annotations;
        private final Invoker invoker;

        private DirectMethod(String name,
                             Class<?> returnType,
                             Class<?>[] parameterTypes,
                             boolean varArgs,
                             Annotation[] annotations,
                             Invoker invoker) {
            this.name = name;
            this.returnType = returnType;
            this.parameterTypes = parameterTypes;
            this.varArgs = varArgs;
            this.annotations = annotations;
            this.invoker = invoker;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Argument<?> getReturnType() {
            return Argument.of(returnType);
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
        public Annotation[] synthesizeAnnotations() {
            return annotations.clone();
        }

        @Override
        public @Nullable Object invoke(ELContext context,
                                       @Nullable Object base,
                                       Object @Nullable [] arguments) {
            try {
                return invoker.invoke(base, coerce(context, arguments));
            } catch (ELException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new ELException(e);
            }
        }

        private int score(ELContext context,
                          Argument<?> @Nullable [] providedArguments,
                          Object @Nullable [] arguments) {
            Object[] values = arguments == null ? new Object[0] : arguments;
            Class<?>[] types = providedArguments == null ? Arrays.stream(values)
                .map(value -> value == null ? null : value.getClass())
                .toArray(Class<?>[]::new) : Argument.toClassArray(providedArguments);
            int fixed = varArgs ? parameterTypes.length - 1 : parameterTypes.length;
            if (varArgs ? types.length < fixed : types.length != fixed) {
                return Integer.MAX_VALUE;
            }
            int score = varArgs ? 10 : 0;
            for (int i = 0; i < fixed; i++) {
                int parameterScore = score(context, parameterTypes[i], types[i], i < values.length ? values[i] : null);
                if (parameterScore == Integer.MAX_VALUE) {
                    return parameterScore;
                }
                score += parameterScore;
            }
            if (varArgs) {
                Class<?> componentType = parameterTypes[fixed].getComponentType();
                for (int i = fixed; i < types.length; i++) {
                    int parameterScore = score(context, componentType, types[i], i < values.length ? values[i] : null);
                    if (parameterScore == Integer.MAX_VALUE) {
                        return parameterScore;
                    }
                    score += parameterScore;
                }
            }
            return score;
        }

        private Object[] coerce(ELContext context, Object @Nullable [] arguments) {
            Object[] values = arguments == null ? new Object[0] : arguments;
            int fixed = varArgs ? parameterTypes.length - 1 : parameterTypes.length;
            if (varArgs) {
                Object[] coerced = new Object[parameterTypes.length];
                for (int i = 0; i < fixed; i++) {
                    coerced[i] = ELSupport.coerceToType(context, values[i], parameterTypes[i]);
                }
                String[] tail = new String[Math.max(0, values.length - fixed)];
                for (int i = fixed; i < values.length; i++) {
                    tail[i - fixed] = ELSupport.coerceToType(context, values[i], String.class);
                }
                coerced[fixed] = tail;
                return coerced;
            }
            Object[] coerced = new Object[parameterTypes.length];
            for (int i = 0; i < parameterTypes.length; i++) {
                coerced[i] = ELSupport.coerceToType(context, values[i], parameterTypes[i]);
            }
            return coerced;
        }

        private static int score(ELContext context,
                                 Class<?> parameterType,
                                 @Nullable Class<?> providedType,
                                 @Nullable Object value) {
            if (providedType != null && wrap(parameterType) == wrap(providedType)) {
                return 0;
            }
            if (providedType != null && wrap(parameterType).isAssignableFrom(wrap(providedType))) {
                return 1;
            }
            try {
                ELSupport.coerceToType(context, value, parameterType);
                return 2;
            } catch (ELException | IllegalArgumentException e) {
                return Integer.MAX_VALUE;
            }
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
    }

    private enum DeprecatedLiteral implements Deprecated {
        INSTANCE;

        @Override
        public String since() {
            return "";
        }

        @Override
        public boolean forRemoval() {
            return false;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return Deprecated.class;
        }
    }
}
