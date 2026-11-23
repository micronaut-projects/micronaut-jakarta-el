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
package io.micronaut.el.test;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.ValueExpression;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The compile-time generated counterparts of the interpreted tests of
 * {@code io.micronaut.el.interpreter.RuntimeVarargsSpreadTest}.
 */
class RuntimeVarargsSpreadExpressionsTest {

    private final ApplicationContext beanContext = ApplicationContext.run();

    @AfterEach
    void close() {
        beanContext.close();
    }

    @Test
    void aStaticMethodTakesASubtypeArrayAsItsVariableArityArray() {
        assertEquals("a-b", value(RuntimeVarargsSpreadExpressions$ELExpressions.STATIC_FORMAT));
    }

    @Test
    void aBeanMethodTakesAnAssignableArrayAsItsVariableArityArray() {
        assertEquals("2:java.lang.String", value(RuntimeVarargsSpreadExpressions$ELExpressions.SUBTYPE_ARRAY));
        assertEquals("2:java.lang.String", value(RuntimeVarargsSpreadExpressions$ELExpressions.EXACT_ARRAY));
        assertEquals("2:java.lang.String", value(RuntimeVarargsSpreadExpressions$ELExpressions.LOOSE_ARGUMENTS));
        assertEquals("2:a-b", value(RuntimeVarargsSpreadExpressions$ELExpressions.FIXED_THEN_SUBTYPE_ARRAY));
    }

    @Test
    void anArrayThatIsNotAssignableOrANullIsTheSingleElement() {
        assertEquals("1:int[]", value(RuntimeVarargsSpreadExpressions$ELExpressions.PRIMITIVE_ARRAY));
        assertEquals("1:null", value(RuntimeVarargsSpreadExpressions$ELExpressions.NULL_ARGUMENT));
    }

    @Test
    void theVariableArityOverloadIsTheMostSpecificForAnAssignableArray() {
        assertEquals("varargs", value(RuntimeVarargsSpreadExpressions$ELExpressions.VARIABLE_ARITY_OVERLOAD));
    }

    @Test
    void aConstructorTakesASubtypeArrayAsItsVariableArityArray() {
        assertEquals("2:a,b", value(RuntimeVarargsSpreadExpressions$ELExpressions.CONSTRUCTOR));
    }

    @Test
    void aFunctionTakesASubtypeArrayAsItsVariableArityArray() {
        assertEquals("2:java.lang.String", value(RuntimeVarargsSpreadExpressions$ELExpressions.FUNCTION));
    }

    @Test
    void theIntrospectionMetadataTakesASubtypeArrayAsItsVariableArityArray() {
        assertEquals("2:java.lang.String", value(RuntimeVarargsSpreadExpressions$ELExpressions.INTROSPECTED));
    }

    @Test
    void theExecutableMethodMetadataTakesASubtypeArrayAsItsVariableArityArray() {
        assertEquals("2:java.lang.String", value(RuntimeVarargsSpreadExpressions$ELExpressions.EXECUTABLE));
    }

    static String describe(Object @Nullable [] values) {
        if (values == null) {
            return "null";
        }
        return values.length + ":" + (values.length == 0 ? "-"
            : values[0] == null ? "null" : values[0].getClass().getTypeName());
    }

    private Object value(ValueExpression expression) {
        ELContext context = new CompiledELContext(beanContext)
            .setBean("strs", new String[]{"a", "b"})
            .setBean("objs", new Object[]{"a", "b"})
            .setBean("ints", new int[]{1, 2})
            .setBean("plain", new SpreadPlain())
            .setBean("introspected", new SpreadIntrospected())
            .setBean("executable", beanContext.getBean(SpreadExecutable.class));
        return expression.getValue(context);
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
