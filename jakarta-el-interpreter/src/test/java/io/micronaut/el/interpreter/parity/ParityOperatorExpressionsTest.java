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

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.micronaut.el.interpreter.parity.ParityEvaluation.value;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The interpreted counterparts of {@code io.micronaut.el.test.OperatorExpressions}, with the results
 * {@code OperatorExpressionsTest} asserts for the compiled ones.
 */
class ParityOperatorExpressionsTest {

    private final ELContext context = new CompiledELContext();

    @Test
    void arithmetic() {
        assertEquals(3L, value(context, "${1 + 2}"));
        assertEquals(7L, value(context, "${1 + 2 * 3}"));
        assertEquals(3.5d, value(context, "${7 / 2}"));
        assertEquals(1L, value(context, "${7 mod 2}"));
        assertEquals(-3L, value(context, "${-3}"));
        assertEquals(2.5d, value(context, "${1.5 + 1}"));
        assertEquals(9L, value(context, "${(1 + 2) * 3}"));
    }

    @Test
    void stringConcatenation() {
        assertEquals("abc", value(context, "${'a' += 'b' += 'c'}"));
    }

    @Test
    void relationalAndLogical() {
        assertEquals(true, value(context, "${1 lt 2}"));
        assertEquals(true, value(context, "${'10' == 10}"));
        assertEquals(true, value(context, "${null == null}"));
        assertEquals(false, value(context, "${true and false}"));
        assertEquals(true, value(context, "${true or false}"));
        assertEquals(false, value(context, "${not true}"));
    }

    @Test
    void emptyOperator() {
        assertEquals(true, value(context, "${empty ''}"));
        assertEquals(false, value(context, "${empty [1]}"));
    }

    @Test
    void conditionalAndSemicolon() {
        assertEquals("yes", value(context, "${true ? 'yes' : 'no'}"));
        assertEquals(3L, value(context, "${1; 2; 3}"));
    }

    @Test
    void collectionConstruction() {
        assertEquals(List.of(1L, "two", 3L), value(context, "${[1, 'two', 3]}"));
        assertEquals(Set.of(1L, 2L, 3L), value(context, "${{1, 2, 2, 3}}"));
        assertEquals(Map.of("one", 1L, "two", 2L), value(context, "${{'one':1, 'two':2}}"));
    }
}
