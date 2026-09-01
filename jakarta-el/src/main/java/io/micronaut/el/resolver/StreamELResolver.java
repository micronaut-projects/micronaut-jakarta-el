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
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.runtime.ELArray;
import io.micronaut.el.runtime.ELSupport;
import org.jspecify.annotations.Nullable;
import io.micronaut.el.stream.ELOptional;
import io.micronaut.el.stream.ELStream;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.LambdaExpression;
import jakarta.el.MethodInfo;

import java.util.Collection;
import java.util.List;

/**
 * The {@link ELResolver} implementing the operations on collection objects described in the chapter 2 of
 * the Jakarta Expression Language specification.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class StreamELResolver extends ELResolver implements ELMethodExecutor {

    private static final Object[] NO_ARGUMENTS = new Object[0];
    private static final String STREAM = "stream";

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    @Nullable
    public Object getValue(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        // the stream operations are read only
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return false;
    }

    @Override
    @Nullable
    public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
        ELMethod resolved = resolve(context, base, method, paramTypes, params);
        if (resolved == null) {
            return null;
        }
        context.setPropertyResolved(base, method);
        return resolved.invoke(context, base, params);
    }

    @Override
    @Nullable
    public ELMethod resolve(ELContext context, @Nullable Object base, @Nullable Object method,
                            Class<?> @Nullable [] paramTypes, Object @Nullable [] params) {
        if (base == null || method == null) {
            return null;
        }
        String name = ELSupport.coerceToString(method);
        int arity = params == null ? paramTypes == null ? 0 : paramTypes.length : params.length;
        if (base instanceof ELStream<?>) {
            return StreamMethod.forName(name, arity);
        }
        if (base instanceof ELOptional) {
            return OptionalMethod.forName(name, arity);
        }
        return STREAM.equals(name) && arity == 0 && isStreamable(base)
            ? StreamMethod.SOURCE : null;
    }

    @Override
    @Nullable
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }

    private static boolean isStreamable(Object base) {
        return base instanceof Collection<?> || ELArray.isArray(base);
    }

    private enum StreamMethod implements ELMethod {
        SOURCE("stream", ELStream.class),
        FILTER("filter", ELStream.class, LambdaExpression.class),
        MAP("map", ELStream.class, LambdaExpression.class),
        FLAT_MAP("flatMap", ELStream.class, LambdaExpression.class),
        DISTINCT("distinct", ELStream.class),
        SORTED_NO_ARGS("sorted", ELStream.class),
        SORTED("sorted", ELStream.class, LambdaExpression.class),
        FOR_EACH("forEach", void.class, LambdaExpression.class),
        PEEK("peek", ELStream.class, LambdaExpression.class),
        ITERATOR("iterator", java.util.Iterator.class),
        LIMIT("limit", ELStream.class, Object.class),
        SUBSTREAM_ONE("substream", ELStream.class, Object.class),
        SUBSTREAM_TWO("substream", ELStream.class, Object.class, Object.class),
        TO_ARRAY("toArray", Object[].class),
        TO_LIST("toList", List.class),
        REDUCE_ONE("reduce", ELOptional.class, LambdaExpression.class),
        REDUCE_TWO("reduce", ELOptional.class, Object.class, LambdaExpression.class),
        MAX_NO_ARGS("max", ELOptional.class),
        MAX("max", ELOptional.class, LambdaExpression.class),
        MIN_NO_ARGS("min", ELOptional.class),
        MIN("min", ELOptional.class, LambdaExpression.class),
        AVERAGE("average", ELOptional.class),
        SUM("sum", long.class),
        COUNT("count", long.class),
        ANY_MATCH("anyMatch", ELOptional.class, LambdaExpression.class),
        ALL_MATCH("allMatch", ELOptional.class, LambdaExpression.class),
        NONE_MATCH("noneMatch", ELOptional.class, LambdaExpression.class),
        FIND_FIRST("findFirst", ELOptional.class);

        private final MethodInfo methodInfo;

        StreamMethod(String name, Class<?> returnType, Class<?>... parameterTypes) {
            this.methodInfo = new MethodInfo(name, returnType, parameterTypes);
        }

        @Nullable
        private static StreamMethod forName(String name, int arity) {
            return switch (name) {
                case "stream" -> null;
                case "filter" -> FILTER;
                case "map" -> MAP;
                case "flatMap" -> FLAT_MAP;
                case "distinct" -> DISTINCT;
                case "sorted" -> arity == 0 ? SORTED_NO_ARGS : arity == 1 ? SORTED : null;
                case "forEach" -> FOR_EACH;
                case "peek" -> PEEK;
                case "iterator" -> ITERATOR;
                case "limit" -> LIMIT;
                case "substream" -> arity == 1 ? SUBSTREAM_ONE : arity == 2 ? SUBSTREAM_TWO : null;
                case "toArray" -> TO_ARRAY;
                case "toList" -> TO_LIST;
                case "reduce" -> arity == 1 ? REDUCE_ONE : arity == 2 ? REDUCE_TWO : null;
                case "max" -> arity == 0 ? MAX_NO_ARGS : arity == 1 ? MAX : null;
                case "min" -> arity == 0 ? MIN_NO_ARGS : arity == 1 ? MIN : null;
                case "average" -> AVERAGE;
                case "sum" -> SUM;
                case "count" -> COUNT;
                case "anyMatch" -> ANY_MATCH;
                case "allMatch" -> ALL_MATCH;
                case "noneMatch" -> NONE_MATCH;
                case "findFirst" -> FIND_FIRST;
                default -> null;
            };
        }

        @Override
        public String getName() {
            return methodInfo.getName();
        }

        @Override
        public Class<?> getReturnType() {
            return methodInfo.getReturnType();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return methodInfo.getParamTypes();
        }

        @Override
        public boolean isVarArgs() {
            return false;
        }

        @Override
        @Nullable
        public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
            if (base instanceof ELStream<?> stream) {
                return stream.invokeOperation(getName(), arguments == null ? NO_ARGUMENTS : arguments);
            }
            if (base != null && getName().equals(STREAM)) {
                return ELStream.of(context, base);
            }
            throw new IllegalArgumentException("The method '" + getName() + "' requires a stream");
        }
    }

    private enum OptionalMethod implements ELMethod {
        GET("get", Object.class),
        IS_PRESENT("isPresent", boolean.class),
        IF_PRESENT("ifPresent", void.class, LambdaExpression.class),
        OR_ELSE("orElse", Object.class, Object.class),
        OR_ELSE_GET("orElseGet", Object.class, LambdaExpression.class);

        private final MethodInfo methodInfo;

        OptionalMethod(String name, Class<?> returnType, Class<?>... parameterTypes) {
            this.methodInfo = new MethodInfo(name, returnType, parameterTypes);
        }

        @Nullable
        private static OptionalMethod forName(String name, int arity) {
            return switch (name) {
                case "get" -> arity == 0 ? GET : null;
                case "isPresent" -> arity == 0 ? IS_PRESENT : null;
                case "ifPresent" -> arity == 1 ? IF_PRESENT : null;
                case "orElse" -> arity == 1 ? OR_ELSE : null;
                case "orElseGet" -> arity == 1 ? OR_ELSE_GET : null;
                default -> null;
            };
        }

        @Override
        public String getName() {
            return methodInfo.getName();
        }

        @Override
        public Class<?> getReturnType() {
            return methodInfo.getReturnType();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return methodInfo.getParamTypes();
        }

        @Override
        public boolean isVarArgs() {
            return false;
        }

        @Override
        @Nullable
        public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
            if (base instanceof ELOptional optional) {
                return optional.invokeOperation(context, getName(), arguments == null ? NO_ARGUMENTS : arguments);
            }
            throw new IllegalArgumentException("The method '" + getName() + "' requires an optional");
        }
    }
}
