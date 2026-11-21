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
package io.micronaut.el.interpreter;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.ELMethodExecutor;
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The single array argument of a variable arity parameter, resolved at runtime. The section 1.2.1.2 of the
 * specification selects a method as the section 15.12.2 of the Java Language Specification does: in its first
 * phase a variable arity method {@code m(T...)} is applicable by subtyping, as the fixed arity {@code m(T[])}, to
 * an array assignable to {@code T[]}, which is therefore the variable arity array itself and not its single
 * element. A {@code String[]} passed to {@code Object...} is two arguments, as in Java.
 *
 * <p>Expressly and Tomcat only pass an array of exactly the parameter type through, and wrap a {@code String[]};
 * the differential test of the fuzz module asserts this deviation separately. Every runtime selection path is
 * exercised: the reflective resolution of a plain bean, of a static method, of a constructor and of a function,
 * and the introspection and executable method metadata Micronaut generates, without the reflective fallback.</p>
 *
 * <p>Every expression here has a compile-time generated counterpart, with the same text and the same expected
 * result, in {@code io.micronaut.el.test.RuntimeVarargsSpreadExpressions} of the Java test suite.</p>
 */
class RuntimeVarargsSpreadTest {

    private final ApplicationContext beanContext = ApplicationContext.run();

    private final ExpressionFactory factory =
        new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser());

    @AfterEach
    void close() {
        beanContext.close();
    }

    @Test
    void aStaticMethodTakesASubtypeArrayAsItsVariableArityArray() {
        assertEquals("a-b", evaluate(factory, context(), "${String.format('%s-%s', strs)}"));
    }

    @Test
    void aBeanMethodTakesAnAssignableArrayAsItsVariableArityArray() {
        ELContext context = context();

        assertEquals("2:java.lang.String", evaluate(factory, context, "${plain.argumentType(strs)}"));
        assertEquals("2:java.lang.String", evaluate(factory, context, "${plain.argumentType(objs)}"));
        assertEquals("2:java.lang.String", evaluate(factory, context, "${plain.argumentType('a', 'b')}"));
        assertEquals("2:a-b", evaluate(factory, context, "${plain.join('-', strs)}"));
    }

    @Test
    void anArrayThatIsNotAssignableOrANullIsTheSingleElement() {
        ELContext context = context();

        assertEquals("1:int[]", evaluate(factory, context, "${plain.argumentType(ints)}"));
        assertEquals("1:null", evaluate(factory, context, "${plain.argumentType(null)}"));
    }

    @Test
    void theVariableArityOverloadIsTheMostSpecificForAnAssignableArray() {
        // both pick(Object) and pick(Object...) apply in the first phase, and Object[] is more specific than Object
        assertEquals("varargs", evaluate(factory, context(), "${plain.pick(strs)}"));
    }

    @Test
    void aConstructorTakesASubtypeArrayAsItsVariableArityArray() {
        assertEquals("2:a,b", evaluate(factory, context(), "${RuntimeSpreadConstructed(strs).value}"));
    }

    @Test
    void aFunctionTakesASubtypeArrayAsItsVariableArityArray() {
        assertEquals("2:java.lang.String", evaluate(factory, context(), "${spread:argumentType(strs)}"));
    }

    @Test
    void theIntrospectionMetadataTakesASubtypeArrayAsItsVariableArityArray() {
        // without the reflective executor, so the introspection of the bean is what dispatches the call
        ExpressionFactory unreflected = new CompiledExpressionFactory(List.of(),
            new InterpretingELExpressionParser(unreflectedExecutors()));

        assertEquals("2:java.lang.String", evaluate(unreflected, context(), "${introspected.argumentType(strs)}"));
    }

    @Test
    void theExecutableMethodMetadataTakesASubtypeArrayAsItsVariableArityArray() {
        // without the reflective executor, so the executable method of the bean definition dispatches the call
        ExpressionFactory unreflected = new CompiledExpressionFactory(List.of(),
            new InterpretingELExpressionParser(unreflectedExecutors()));

        assertEquals("2:java.lang.String", evaluate(unreflected, context(), "${executable.argumentType(strs)}"));
    }

    static String describe(Object @Nullable [] values) {
        if (values == null) {
            return "null";
        }
        return values.length + ":" + (values.length == 0 ? "-"
            : values[0] == null ? "null" : values[0].getClass().getTypeName());
    }

    private ELContext context() {
        SpreadContext context = new SpreadContext(beanContext);
        context.setBean("strs", new String[]{"a", "b"});
        context.setBean("objs", new Object[]{"a", "b"});
        context.setBean("ints", new int[]{1, 2});
        context.setBean("plain", new SpreadPlain());
        context.setBean("introspected", new SpreadIntrospected());
        context.setBean("executable", beanContext.getBean(SpreadExecutable.class));
        context.getImportHandler().importClass(RuntimeSpreadConstructed.class.getName());
        return context;
    }

    private static Object evaluate(ExpressionFactory factory, ELContext context, String expression) {
        return factory.createValueExpression(context, expression, Object.class).getValue(context);
    }

    private static List<ELMethodExecutor> unreflectedExecutors() {
        List<ELMethodExecutor> executors = new ArrayList<>(
            SoftServiceLoader.load(ELMethodExecutor.class).collectAll());
        executors.removeIf(ELMethodExecutor::isReflective);
        return executors;
    }

    /**
     * A context mapping the {@code spread} functions.
     */
    private static final class SpreadContext extends CompiledELContext {

        private SpreadContext(ApplicationContext beanContext) {
            super(beanContext);
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return new FunctionMapper() {
                @Override
                public Method resolveFunction(String prefix, String localName) {
                    if (!"spread".equals(prefix) || !"argumentType".equals(localName)) {
                        return null;
                    }
                    try {
                        return RuntimeSpreadFunctions.class.getMethod("argumentType", Object[].class);
                    } catch (NoSuchMethodException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
        }
    }

    /**
     * A bean that is neither introspected nor a bean of the context, resolved reflectively.
     */
    public static final class SpreadPlain {

        public String argumentType(Object... values) {
            return describe(values);
        }

        public String join(String separator, Object... parts) {
            return parts.length + ":" + Arrays.stream(parts).map(String::valueOf)
                .collect(Collectors.joining(separator));
        }

        public String pick(Object value) {
            return "object";
        }

        public String pick(Object... values) {
            return "varargs";
        }
    }

    /**
     * An introspected bean, whose methods the introspection describes.
     */
    @Introspected
    public static class SpreadIntrospected {

        @Executable
        public String argumentType(Object... values) {
            return describe(values);
        }
    }

    /**
     * A bean of the context, whose methods its definition describes as executable methods.
     */
    @Singleton
    public static class SpreadExecutable {

        @Executable
        public String argumentType(Object... values) {
            return describe(values);
        }
    }
}
