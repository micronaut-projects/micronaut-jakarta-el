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

import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.ELSandbox;
import jakarta.el.ELContext;
import jakarta.el.ELProcessor;
import jakarta.el.ExpressionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The interpreted counterparts of {@code io.micronaut.el.test.VarargsSpreadExpressions}, with the same text and
 * the same results: an array passed as the single argument of a variable arity parameter {@code T...}, to which it
 * is assignable, is the array of the parameter, not its one element, section 1.2.1.2 of the specification
 * following section 15.12.2 of the JLS, whatever the kind of invocation.
 */
class VarargsSpreadInterpretedTest {

    private final ExpressionFactory factory = new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser());
    private final ELProcessor processor = new ELProcessor();

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        processor.defineBean("bean", new VarargsSpreadBean());
        processor.defineBean("strings", new String[]{"a", "b"});
        processor.defineBean("objects", new Object[]{"a", "b"});
        processor.defineBean("numbers", new int[]{1, 2});
        processor.getELManager().importClass(VarargsSpreadBean.class.getName());
        processor.getELManager().importClass(VarargsSpreadTally.class.getName());
        processor.getELManager().importStatic(VarargsSpreadFunctions.class.getName() + ".count");
        processor.defineFunction("vs", "count", VarargsSpreadFunctions.class.getMethod("count", Object[].class));
        // the sandbox is a policy of the interpreted path, not a semantic difference, and this compares semantics
        processor.getELManager().getELContext().putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);
    }

    private Object value(String expression) {
        ELContext context = processor.getELManager().getELContext();
        return factory.createValueExpression(context, expression, Object.class).getValue(context);
    }

    @Test
    void aStaticMethodOfTheJdkSpreadsTheArray() {
        assertEquals("a-b", value("${String.format('%s-%s', strings)}"));
    }

    @Test
    void anInstanceMethodSpreadsTheArray() {
        assertEquals("2:java.lang.String", value("${bean.argumentType(strings)}"));
        assertEquals("a-b", value("${bean.join('-', strings)}"));
    }

    @Test
    void aStaticMethodSpreadsTheArray() {
        assertEquals("2:java.lang.String", value("${VarargsSpreadBean.staticCount(strings)}"));
        assertEquals("2:java.lang.String", value("${bean.staticCount(strings)}"));
    }

    @Test
    void aFunctionSpreadsTheArray() {
        assertEquals("2:java.lang.String", value("${vs:count(strings)}"));
        assertEquals("2:java.lang.String", value("${count(strings)}"));
    }

    @Test
    void aConstructorSpreadsTheArray() {
        assertEquals("2:java.lang.String", value("${VarargsSpreadTally(strings).summary}"));
    }

    @Test
    void anArrayOfTheParameterTypeIsSpreadAndAPrimitiveArrayIsPacked() {
        assertEquals("2:java.lang.String", value("${bean.argumentType(objects)}"));
        assertEquals("1:int[]", value("${bean.argumentType(numbers)}"));
    }
}
