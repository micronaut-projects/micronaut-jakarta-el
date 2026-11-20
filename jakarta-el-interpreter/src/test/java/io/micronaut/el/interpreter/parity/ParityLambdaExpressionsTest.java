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
import jakarta.el.ELException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.micronaut.el.interpreter.parity.ParityEvaluation.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The interpreted counterparts of {@code io.micronaut.el.test.LambdaExpressions}, with the results
 * {@code LambdaExpressionsTest} asserts for the compiled ones.
 *
 * <p>{@code ${item.note.map(n -> n.length()).orElse(0)}} calls a method on a {@link java.util.Optional}, whose
 * interpreted resolution is being changed in parallel; its pair is added with that change.</p>
 */
class ParityLambdaExpressionsTest {

    private final ELContext context = new CompiledELContext()
        .setBean("item", new ParityInventory("A-1", 5, 10L, 9.99d, 1.5f, true, Suit.HEART));

    @Test
    void streamOperationsTakeLambdas() {
        assertEquals(List.of("new!", "sale!"),
            value(context, "${item.tags.stream().filter(t -> t.length() > 1).map(t -> t += '!').toList()}"));
        assertEquals(List.of("sale", "new", "b"),
            value(context, "${item.tags.stream().sorted((a, b) -> b.length() - a.length()).toList()}"));
        assertEquals(true, value(context, "${item.tags.stream().anyMatch(t -> t == 'b').get()}"));
        assertEquals("newsaleb", value(context, "${item.tags.stream().reduce('', (a, b) -> a += b)}"));
        assertEquals("fallback",
            value(context, "${item.tags.stream().filter(t -> t == 'none').findFirst().orElseGet(() -> 'fallback')}"));
        assertNull(value(context, "${item.tags.stream().forEach(t -> t.length())}"));
    }

    @Test
    void functionalInterfacesOfBeansTakeLambdas() {
        assertEquals(2L, value(context, "${item.count(t -> t.length() > 1)}"));
        assertEquals("A-1:5", value(context, "${item.describe(i -> i.sku += ':' += i.quantity)}"));
        assertEquals(9.99d * 5 + 1, value(context, "${item.adjusted((p, q) -> p * q + 1)}"));
    }

    @Test
    void lambdaValues() {
        assertEquals(10L, value(context, "${(x -> x * 2)(item.quantity)}"));
        assertEquals(3L, value(context, "${f = (x, y) -> x + y; f(1, 2)}"));
        assertEquals(3L, value(context, "${(x -> y -> x + y)(1)(2)}"));
        assertEquals(10L, value(context, "${((a, b, c, d) -> a + b + c + d)(1, 2, 3, 4)}"));
        assertEquals(5, value(context, "${(() -> item.quantity)()}"));
        assertEquals(List.of(6L, 4L), value(context, "${[3, 1, 2].stream().filter(n -> n > 1).map(n -> n * 2).toList()}"));
    }

    @Test
    void aMissingArgumentIsReported() {
        ELException e = assertThrows(ELException.class, () -> value(context, "${((x, y) -> x + y)(1)}"));
        assertEquals("Expected Argument y missing in Lambda Expression", e.getMessage());
    }
}
