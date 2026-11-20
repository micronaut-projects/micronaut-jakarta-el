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
 * The interpreted counterparts of {@code io.micronaut.el.test.InlinedOperatorExpressions}. The compiler inlines
 * the operators applied to the primitive properties; the interpreter applies the operators of the specification
 * to the boxed values, and must arrive at the same results as {@code InlinedOperatorExpressionsTest}.
 */
class ParityInlinedOperatorExpressionsTest {

    private final ELContext context = new CompiledELContext()
        .setBean("item", new ParityInventory("A-1", 5, 10L, 9.99d, 1.5f, true, Suit.HEART));
    private final ELContext unnamed = new CompiledELContext()
        .setBean("item", new ParityInventory(null, 5, 10L, 9.99d, 1.5f, false, Suit.SPADE));

    @Test
    void arithmetic() {
        assertEquals(6L, value(context, "${item.quantity + 1}"));
        assertEquals(15L, value(context, "${item.quantity + item.reorderLevel}"));
        assertEquals(49.95d, value(context, "${item.quantity * item.price}"));
        assertEquals(8.49d, value(context, "${item.price - item.weight}"));
        assertEquals(2.5d, value(context, "${item.quantity / 2}"));
        assertEquals(1L, value(context, "${item.quantity mod 2}"));
        assertEquals(9.99d % 2, value(context, "${item.price mod 2}"));
    }

    @Test
    void negationKeepsTheTypeOfTheOperand() {
        assertEquals(-5, value(context, "${-item.quantity}"));
        assertEquals(-10L, value(context, "${-item.reorderLevel}"));
        assertEquals(-9.99d, value(context, "${-item.price}"));
        assertEquals(-1.5f, value(context, "${-item.weight}"));
    }

    @Test
    void comparisons() {
        assertEquals(true, value(context, "${item.quantity > 3}"));
        assertEquals(true, value(context, "${item.quantity <= item.reorderLevel}"));
        assertEquals(true, value(context, "${item.quantity == 5}"));
        assertEquals(false, value(context, "${item.quantity != 5}"));
        assertEquals(true, value(context, "${item.price >= 9.99}"));
        assertEquals(true, value(context, "${item.weight < item.price}"));
        assertEquals(true, value(context, "${item.quantity == 5.0}"));
    }

    @Test
    void floatingPointComparisonsFollowTheRuntime() {
        assertEquals(true, value(context, "${item.nan > 1}"));
        assertEquals(false, value(context, "${item.nan < 1}"));
        assertEquals(true, value(context, "${item.nan == item.nan}"));
    }

    @Test
    void equalityOfStringsAndBooleans() {
        assertEquals(true, value(context, "${item.sku == 'A-1'}"));
        assertEquals(false, value(context, "${item.sku != 'A-1'}"));
        assertEquals(false, value(unnamed, "${item.sku == 'A-1'}"));
        assertEquals(true, value(unnamed, "${item.sku != 'A-1'}"));
        assertEquals(true, value(context, "${item.available == true}"));
        assertEquals(false, value(unnamed, "${item.available == true}"));
    }

    @Test
    void logical() {
        assertEquals(false, value(context, "${not item.available}"));
        assertEquals(true, value(context, "${item.available and item.quantity > 0}"));
        assertEquals(false, value(unnamed, "${item.available and item.quantity > 0}"));
        assertEquals(true, value(unnamed, "${item.available or item.quantity > 0}"));
        assertEquals("in stock", value(context, "${item.available ? 'in stock' : 'sold out'}"));
        assertEquals("sold out", value(unnamed, "${item.available ? 'in stock' : 'sold out'}"));
    }

    @Test
    void stringConcatenationCoercesTheOperands() {
        assertEquals("A-1 x5", value(context, "${item.sku += ' x' += item.quantity}"));
        assertEquals(" x5", value(unnamed, "${item.sku += ' x' += item.quantity}"));
        String composite = "SKU ${item.sku} qty ${item.quantity} suit ${item.suit} weight ${item.weight} ok ${item.available}";
        assertEquals("SKU A-1 qty 5 suit HEART weight 1.5 ok true", value(context, composite));
        assertEquals("SKU  qty 5 suit SPADE weight 1.5 ok false", value(unnamed, composite));
    }
}
