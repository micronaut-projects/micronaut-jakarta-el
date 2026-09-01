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
import io.micronaut.el.ELMethodContributor;
import io.micronaut.el.ELMethodRegistry;
import jakarta.el.ELException;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Vector;
import java.util.function.Predicate;

/**
 * Declares, without reflection, the Java methods the TCK calls from expressions parsed at runtime.
 *
 * <p>Every registration names the overload as the type really declares it, including the bridge
 * {@code compareTo(Object)} the compiler generates for a {@code Comparable}, so that the overload selection of
 * the section 1.6 of the specification picks the same method the reflective resolver would.</p>
 */
@SuppressWarnings("deprecation")
public final class TckELMethodContributor implements ELMethodContributor {

    private static final Annotation[] DEPRECATED = {DeprecatedLiteral.INSTANCE};

    @Override
    public void contribute(ELMethodRegistry registry) {
        collections(registry);
        comparisons(registry);
        methodsBean(registry);
        functions(registry);
    }

    /**
     * {@code java.util.List} mutators, which the resolver tests call through an expression.
     */
    private static void collections(ELMethodRegistry registry) {
        registry.method(Vector.class, "add", boolean.class, new Class<?>[]{Object.class}, false,
                (context, base, arguments) -> vector(base).add(arguments[0]))
            .method(Vector.class, "add", void.class, new Class<?>[]{int.class, Object.class}, false,
                (context, base, arguments) -> {
                    vector(base).add((Integer) arguments[0], arguments[1]);
                    return null;
                })
            .method(ArrayList.class, "add", boolean.class, new Class<?>[]{Object.class}, false,
                (context, base, arguments) -> arrayList(base).add(arguments[0]))
            .method(ArrayList.class, "add", void.class, new Class<?>[]{int.class, Object.class}, false,
                (context, base, arguments) -> {
                    arrayList(base).add((Integer) arguments[0], arguments[1]);
                    return null;
                });
    }

    /**
     * The {@code Comparable} and {@code Object} methods the lambda and coercion tests call. The bridge
     * {@code compareTo(Object)} of each type is declared next to the method it bridges to: an argument that is
     * not of the type of the receiver is assignable to the bridge but only coercible to the typed overload, so
     * the bridge is selected and the coercion of the section 1.23 does not apply, exactly as it does not when
     * the method is found reflectively.
     */
    private static void comparisons(ELMethodRegistry registry) {
        registry.method(Integer.class, "compareTo", int.class, Integer.class, Integer::compareTo)
            .method(Integer.class, "compareTo", int.class, Object.class,
                (base, first) -> compareToBridge(base, first, Integer.class))
            .method(Long.class, "compareTo", int.class, Long.class, Long::compareTo)
            .method(Long.class, "compareTo", int.class, Object.class,
                (base, first) -> compareToBridge(base, first, Long.class))
            .method(String.class, "compareTo", int.class, String.class, String::compareTo)
            .method(String.class, "compareTo", int.class, Object.class,
                (base, first) -> compareToBridge(base, first, String.class))
            .method(String.class, "equals", boolean.class, Object.class, String::equals);
    }

    /**
     * The overloaded and variable arity methods the method expression tests select between.
     */
    private static void methodsBean(ELMethodRegistry registry) {
        registry.method(MethodsBean.class, "targetA", String.class, CharSequence.class, MethodsBean::targetA)
            .method(MethodsBean.class, "targetA", String.class, String.class, MethodsBean::targetA)
            .method(MethodsBean.class, "targetA", String.class, Long.class, MethodsBean::targetA)
            .method(MethodsBean.class, "targetB", String.class, CharSequence.class, MethodsBean::targetB)
            .method(MethodsBean.class, "targetB", String.class, Long.class, MethodsBean::targetB)
            .method(MethodsBean.class, "targetC", String.class, CharSequence.class, CharSequence.class,
                MethodsBean::targetC)
            .method(MethodsBean.class, "targetC", String.class,
                new Class<?>[]{String.class, String[].class}, true,
                (context, base, arguments) -> bean(base).targetC((String) arguments[0], (String[]) arguments[1]))
            .method(MethodsBean.class, "targetD", String.class, Long.class, Long.class, MethodsBean::targetD)
            .method(MethodsBean.class, "targetD", String.class,
                new Class<?>[]{String.class, String[].class}, true,
                (context, base, arguments) -> bean(base).targetD((String) arguments[0], (String[]) arguments[1]))
            .method(MethodsBean.class, "targetE", String.class, Long.class, Long.class, MethodsBean::targetE)
            .method(MethodsBean.class, "targetE", String.class, String.class, String.class, MethodsBean::targetE)
            .method(MethodsBean.class, "targetF", String.class, new Class<?>[]{String.class, Long.class}, false,
                DEPRECATED,
                (context, base, arguments) -> bean(base).targetF((String) arguments[0], (Long) arguments[1]));
    }

    /**
     * The functions the coercion tests define, which the specification would otherwise look up through a
     * {@code jakarta.el.FunctionMapper} and therefore through {@code java.lang.reflect.Method}.
     */
    private static void functions(ELMethodRegistry registry) {
        registry.function("Int", "val", Integer.class, "valueOf", Integer.class, String.class,
                Integer::valueOf)
            .function("", "testPrimitiveBooleanArray", com.sun.ts.tests.el.spec.coercion.ELClientIT.class,
                "testPrimitiveBooleanArray", int.class, new Class<?>[]{boolean[].class}, false,
                (context, base, arguments) -> com.sun.ts.tests.el.spec.coercion.ELClientIT
                    .testPrimitiveBooleanArray((boolean[]) arguments[0]))
            .function("", "testPredicateString", com.sun.ts.tests.el.spec.coercion.ELClientIT.class,
                "testPredicateString", String.class, new Class<?>[]{Predicate.class}, false,
                (context, base, arguments) -> com.sun.ts.tests.el.spec.coercion.ELClientIT
                    .testPredicateString(predicate(arguments[0])))
            .function("", "testPredicateLong", com.sun.ts.tests.el.spec.coercion.ELClientIT.class,
                "testPredicateLong", String.class, new Class<?>[]{Predicate.class}, false,
                (context, base, arguments) -> com.sun.ts.tests.el.spec.coercion.ELClientIT
                    .testPredicateLong(predicate(arguments[0])));
    }

    /**
     * Invokes a bridge {@code compareTo(Object)}. What the cast throws reaches the caller of the expression as
     * an {@code ELException}, which the registry does for every invocation it dispatches.
     */
    private static <T extends Comparable<T>> int compareToBridge(T base,
                                                                 @Nullable Object first,
                                                                 Class<T> type) {
        return base.compareTo(type.cast(first));
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
    private static <T> Predicate<T> predicate(@Nullable Object value) {
        return (Predicate<T>) value;
    }

    /**
     * The {@code @Deprecated} a {@code jakarta.el.MethodReference} reports for {@code MethodsBean.targetF}.
     */
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
