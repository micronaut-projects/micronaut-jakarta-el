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
package io.micronaut.el.interpreter.parity;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import org.junit.jupiter.api.Test;

import static io.micronaut.el.interpreter.parity.ParityEvaluation.value;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code InterpretingELExpressionParserTest} only parses its expressions to exercise the cache. This evaluates
 * them, with the results {@code io.micronaut.el.test.parity.ParityInterpreterExpressionsTest} asserts for their
 * generated counterparts.
 */
class ParityParserExpressionsTest {

    private final ELContext context = new CompiledELContext().setBean("extra", "value");

    @Test
    void theExpressionsOfTheParserCacheRegression() {
        assertEquals(0L, value(context, "${0}"));
        assertEquals("value", value(context, "${extra}"));
    }
}
