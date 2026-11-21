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
 * The interpreted counterparts of {@code io.micronaut.el.test.PrimitiveVariableExpressions}, with the same text
 * and the same results: the value of a variable is an object, an {@code Integer} that selects
 * {@code which(Integer)} over {@code which(int)}, as Expressly and Tomcat select too, and a null that compares to
 * null, is empty and becomes zero for a primitive parameter.
 */
class PrimitiveVariableInterpretedTest {

    private final ExpressionFactory factory = new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser());
    private final ELProcessor processor = new ELProcessor();

    @BeforeEach
    void setUp() {
        processor.defineBean("bean", new PrimitiveVariableBean());
        processor.defineBean("count", 2);
        processor.getELManager().getELContext().getVariableMapper().setVariable("none",
            factory.createValueExpression(null, Object.class));
        // the sandbox is a policy of the interpreted path, not a semantic difference, and this compares semantics
        processor.getELManager().getELContext().putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);
    }

    private Object value(String expression) {
        ELContext context = processor.getELManager().getELContext();
        return factory.createValueExpression(context, expression, Object.class).getValue(context);
    }

    @Test
    void aPrimitiveValueSelectsTheWrapperOverload() {
        assertEquals("Integer:2", value("${bean.which(count)}"));
        assertEquals("Integer:2", value("${bean.which(bean.count)}"));
        assertEquals("Long:2", value("${bean.which(count + 0)}"));
    }

    @Test
    void aPrimitiveVariableIsStillCoercedToAPrimitiveParameter() {
        assertEquals("abab", value("${bean.repeat(count)}"));
        assertEquals("", value("${bean.repeat(none)}"));
        assertEquals(3L, value("${count + 1}"));
    }

    @Test
    void aNullPrimitiveVariableIsNull() {
        assertEquals(1L, value("${none + 1}"));
        assertEquals(true, value("${none == null}"));
        assertEquals(true, value("${empty none}"));
    }
}
