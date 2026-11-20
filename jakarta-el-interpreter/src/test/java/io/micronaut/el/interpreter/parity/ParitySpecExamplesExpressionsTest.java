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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.micronaut.el.interpreter.parity.ParityEvaluation.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The interpreted counterparts of {@code io.micronaut.el.test.SpecExamplesExpressions}: the same expression text
 * against the same beans, with the results {@code SpecExamplesExpressionsTest} asserts for the compiled ones.
 */
class ParitySpecExamplesExpressionsTest {

    private final CompiledELContext context = new CompiledELContext()
        .setBean("item", new ParityInventory("A-1", 5, 10L, 9.99d, 1.5f, true, Suit.HEART))
        .setBean("prices", Map.of("widget", 2.5d));

    {
        context.getImportHandler().importClass(Suit.class.getName());
    }

    @Test
    void literalAndCompositeExpressions() {
        assertEquals("plain text", value(context, "plain text", String.class));
        assertEquals("${not an expression}", value(context, "\\${not an expression}", String.class));
        assertEquals("A-1 has 5 in HEART", value(context, "${item.sku} has ${item.quantity} in ${item.suit}", String.class));
    }

    @Test
    void literals() {
        assertEquals(42L, value(context, "${42}"));
        assertEquals(4.2d, value(context, "${4.2}"));
        assertEquals(1000d, value(context, "${1e3}"));
        assertEquals("it's", value(context, "${'it\\'s'}"));
        assertEquals("double \"quoted\"", value(context, "${\"double \\\"quoted\\\"\"}"));
        assertEquals(true, value(context, "${true}"));
        assertNull(value(context, "${null}"));
    }

    @Test
    void propertiesMethodsAndArithmetic() {
        assertEquals("A-1", value(context, "${item['sku']}"));
        assertEquals("sale", value(context, "${item.tags[1]}"));
        assertEquals(2.5d, value(context, "${prices['widget']}"));
        assertEquals(2.5d, value(context, "${prices.widget}"));
        assertEquals(3, value(context, "${item.tags.size()}"));
        assertEquals(5 * 9.99d - 10d / 2, value(context, "${item.quantity * item.price - item.reorderLevel / 2}"));
        assertEquals(2.5d, value(context, "${item.quantity div 2}"));
        assertEquals(2L, value(context, "${item.quantity mod 3}"));
        assertEquals(-6L, value(context, "${-(item.quantity + 1)}"));
        assertEquals(15L, value(context, "${'10' + 5}"));
        assertEquals("105", value(context, "${'10' += 5}"));
        assertEquals(1L, value(context, "${null + 1}"));
    }

    @Test
    void relationalLogicalEmptyAndConditional() {
        assertEquals(true, value(context, "${item.quantity eq 5}"));
        assertEquals(false, value(context, "${item.quantity ne 5}"));
        assertEquals(true, value(context, "${item.sku lt 'B'}"));
        assertEquals(true, value(context, "${item.suit == 'HEART'}"));
        assertEquals(true, value(context, "${item.suit == Suit.HEART}"));
        assertEquals(true, value(context, "${'5' == item.quantity}"));
        assertEquals(true, value(context, "${item.available && not item.available || item.quantity > 0}"));
        assertEquals(false, value(context, "${empty item.sku}"));
        assertEquals(false, value(context, "${empty item.tags}"));
        assertEquals(false, value(context, "${empty prices}"));
        assertEquals("many", value(context, "${item.quantity > 3 ? 'many' : 'few'}"));
        assertEquals(5, value(context, "${item.available ? item.quantity : -1}"));
    }

    @Test
    void assignmentAndSemicolon() {
        assertEquals(11L, value(context, "${total = item.quantity * 2; total + 1}"));
    }

    @Test
    void collectionsAndStaticReferences() {
        assertEquals(List.of(1L, 2L, 3L), value(context, "${[1, 2, 3]}"));
        assertEquals(Set.of(1L, 2L), value(context, "${{1, 1, 2}}"));
        assertEquals(Map.of("one", 1L, "two", 2L), value(context, "${{'one': 1, 'two': 2}}"));
        assertEquals(List.of("A-1", 5), value(context, "${[item.sku, item.quantity]}"));
        assertEquals(Suit.SPADE, value(context, "${Suit.SPADE}"));
        assertEquals(Suit.CLUB, value(context, "${Suit.valueOf('CLUB')}"));
        assertEquals(Integer.MAX_VALUE, value(context, "${Integer.MAX_VALUE}"));
        assertEquals(7, value(context, "${Math.max(item.quantity, Integer.valueOf(7))}"));
    }

    @Test
    void streamsAndOptionals() {
        assertEquals(List.of("b", "new", "sale"), value(context, "${item.tags.stream().sorted().toList()}"));
        assertEquals(8L, value(context, "${item.tags.stream().map(t -> t.length()).sum()}"));
        assertEquals(8d / 3, value(context, "${item.tags.stream().map(t -> t.length()).average().get()}"));
        assertEquals(3L, value(context, "${item.tags.stream().distinct().count()}"));
        assertEquals(List.of("new", "sale"), value(context, "${item.tags.stream().limit(2).toList()}"));
        assertEquals(List.of("sale", "b"), value(context, "${item.tags.stream().substream(1, 3).toList()}"));
        assertEquals("new", value(context, "${item.tags.stream().findFirst().orElse('none')}"));
        assertEquals(true, value(context, "${item.tags.stream().allMatch(t -> t.length() > 0).get()}"));
        assertEquals(true, value(context, "${item.tags.stream().noneMatch(t -> t == 'x').get()}"));
        assertEquals("b", value(context, "${item.tags.stream().min().get()}"));
        assertEquals("new,sale,b", value(context, "${item.tags.stream().reduce((a, b) -> a += ',' += b).get()}"));
        assertEquals(List.of(1L, 2L, 3L), value(context, "${[3, 1, 2].stream().sorted((a, b) -> a - b).toList()}"));
        assertEquals(List.of(1L, 2L, 3L), value(context, "${[[1, 2], [3]].stream().flatMap(l -> l.stream()).toList()}"));
    }
}
