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
import io.micronaut.el.parser.ELParserOptions;
import io.micronaut.el.parser.ELParsingException;
import jakarta.el.ELContext;
import jakarta.el.ELProcessor;
import jakarta.el.ExpressionFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The interpreted counterparts of {@code io.micronaut.el.processor.ParserLimitsTest}: the runtime
 * {@link ELParserOptions}, read from the same option names and lowered to the same limits, rejects the same text
 * the processor refuses to compile, and the default limits accept it. The generated side cannot produce an
 * expression past a lowered limit, the compilation fails, so the contract compared is the rejection itself.
 */
class ParserLimitsInterpretedTest {

    private static final String LONG_TEXT = "${'" + "x".repeat(30) + "'}";
    private static final String MANY_TOKENS = "${1+1+1+1+1}";
    private static final String DEEP = "${((1))}";
    private static final String SHALLOW = "${(1+1)}";
    private static final String DEEP_METHOD = "#{bean.join(((1)))}";

    private final ELProcessor processor = new ELProcessor();

    private ExpressionFactory factory(Map<String, String> options) {
        return new CompiledExpressionFactory(List.of(),
            new InterpretingELExpressionParser(ELParserOptions.fromResolver(options::get)));
    }

    private ELContext context() {
        processor.defineBean("bean", new Bean());
        return processor.getELManager().getELContext();
    }

    private Object value(Map<String, String> options, String expression) {
        ELContext context = context();
        return factory(options).createValueExpression(context, expression, Object.class).getValue(context);
    }

    private String rejection(Map<String, String> options, String expression) {
        return assertThrows(ELParsingException.class, () -> value(options, expression)).getMessage();
    }

    @Test
    void anExpressionLongerThanALoweredMaxLengthIsRejected() {
        String error = rejection(Map.of("micronaut.el.parser.max-length", "20"), LONG_TEXT);
        assertTrue(error.contains("An expression cannot exceed 20 characters"), error);
        assertEquals("x".repeat(30), value(Map.of(), LONG_TEXT));
    }

    @Test
    void anExpressionWithMoreTokensThanALoweredMaxTokensIsRejected() {
        String error = rejection(Map.of("micronaut.el.parser.max-tokens", "8"), MANY_TOKENS);
        assertTrue(error.contains("An expression cannot exceed 8 tokens"), error);
        assertEquals(5L, value(Map.of(), MANY_TOKENS));
    }

    @Test
    void anExpressionNestedDeeperThanALoweredMaxDepthIsRejected() {
        String error = rejection(Map.of("micronaut.el.parser.max-depth", "2"), DEEP);
        assertTrue(error.contains("The expression nests more than 2 levels deep"), error);
        assertEquals(1L, value(Map.of(), DEEP));
        assertEquals(2L, value(Map.of("micronaut.el.parser.max-depth", "2"), SHALLOW));
    }

    @Test
    void aTreeTallerThanALoweredMaxTreeHeightIsRejected() {
        String error = rejection(Map.of("micronaut.el.parser.max-tree-height", "3"), MANY_TOKENS);
        assertTrue(error.contains("The syntax tree of an expression cannot be more than 3"), error);
    }

    @Test
    void aMethodExpressionIsHeldToTheSameLimits() {
        ELContext context = context();
        ExpressionFactory lowered = factory(Map.of("micronaut.el.parser.max-depth", "2"));
        String error = assertThrows(ELParsingException.class,
            () -> lowered.createMethodExpression(context, DEEP_METHOD, Object.class, new Class<?>[0])).getMessage();
        assertTrue(error.contains("The expression nests more than 2 levels deep"), error);
    }

    @Test
    void anInvalidOptionIsReported() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> ELParserOptions.fromResolver(Map.of("micronaut.el.parser.max-length", "many")::get));
        assertTrue(error.getMessage().contains("micronaut.el.parser.max-length must be an integer"), error.getMessage());
    }

    /**
     * The bean of the method expression, as declared in the processor test.
     */
    public static class Bean {
        public String join(Object value) {
            return String.valueOf(value);
        }
    }
}
