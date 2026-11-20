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
import jakarta.el.MethodExpression;
import jakarta.el.MethodInfo;
import jakarta.el.MethodReference;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.micronaut.el.interpreter.parity.ParityEvaluation.expression;
import static io.micronaut.el.interpreter.parity.ParityEvaluation.method;
import static io.micronaut.el.interpreter.parity.ParityEvaluation.value;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The interpreted counterparts of the smaller generated holders of {@code io.micronaut.el.test}:
 * {@code InferredTypeExpressions}, {@code EqualityExpressions}, {@code SelectionExpressions},
 * {@code FormattingExpressions}, {@code FactoryExpressions} and {@code LValueExpressions}.
 */
class ParityHolderExpressionsTest {

    private final ParityInventory inventory = new ParityInventory("A-1", 5, 10L, 9.99d, 1.5f, true, Suit.HEART);

    @Test
    void inferredTypeExpressions() {
        ELContext context = new CompiledELContext().setBean("item", inventory);
        assertEquals(9.99d, value(context, "${item.price}", Double.class));
        assertEquals(true, value(context, "${item.quantity > 3}", Boolean.class));
        assertEquals("A-1", value(context, "${item.sku}", String.class));
        assertEquals(List.of("new", "sale", "b"), value(context, "${item.tags}", List.class));
        assertEquals(Suit.HEART, value(context, "${item.suit}", Suit.class));
        assertEquals(2L, method(context, "${item.count(t -> t.length() > 1)}", Long.class, new Class<?>[0])
            .invoke(context, null));

        ELContext unnamed = new CompiledELContext()
            .setBean("item", new ParityInventory(null, 5, 10L, 9.99d, 1.5f, true, Suit.HEART));
        assertEquals("", value(unnamed, "${item.sku}", String.class));
    }

    @Test
    void equalityExpressions() {
        Book book = new Book("EL", "reference", 20d);
        ELContext context = new CompiledELContext().setBean("book", book);

        ValueExpression title = expression(context, "${book.title}", String.class);
        ValueExpression bracket = expression(context, "${book['title']}", String.class);
        ValueExpression cheap = expression(context, "${book.unitPrice lt 10}", Boolean.class);
        ValueExpression cheapSymbol = expression(context, "${book.unitPrice < 10}", Boolean.class);
        ValueExpression cheapReversed = expression(context, "${10 > book.unitPrice}", Boolean.class);
        assertEquals("EL", bracket.getValue(context));
        assertEquals(false, cheap.getValue(context));
        assertEquals(false, cheapSymbol.getValue(context));
        assertEquals(false, cheapReversed.getValue(context));
        assertEquals(title, expression(context, "#{ book.title }", String.class));
        assertEquals(title, bracket);
        assertEquals(title.hashCode(), bracket.hashCode());
        assertEquals(cheap, cheapSymbol);
        assertNotEquals(cheap, cheapReversed);
        assertNotEquals(title, cheap);

        MethodExpression parametered = method(context, "${book.discounted(10)}", Double.class, null);
        assertTrue(parametered.isParametersProvided());
        assertEquals(18d, parametered.invoke(context, null));
        MethodInfo info = parametered.getMethodInfo(context);
        assertEquals("discounted", info.getName());
        assertEquals(double.class, info.getReturnType());
        assertArrayEquals(new Class<?>[]{double.class}, info.getParamTypes());
        MethodReference reference = parametered.getMethodReference(context);
        assertEquals(book, reference.getBase());
        assertArrayEquals(new Object[]{10L}, reference.getEvaluatedParameters());

        MethodExpression declared = method(context, "${book.discounted}", Double.class, new Class<?>[]{Double.class});
        assertEquals(18d, declared.invoke(context, new Object[]{10d}));
        assertEquals("discounted", declared.getMethodInfo(context).getName());
        assertNull(declared.getMethodReference(context).getEvaluatedParameters());
    }

    @Test
    void selectionExpressions() {
        CompiledELContext context = new CompiledELContext().setBean("selection", new ParitySelection());
        context.getImportHandler().importClass(Constructed.class.getName());

        assertEquals("number", value(context, "${Constructed(1).selected}"));
        assertEquals("0", value(context, "${selection.sorted((a, b) -> null)}"));
        assertEquals(2, method(context, "${Math.max}", Object.class, new Class<?>[]{int.class, int.class})
            .invoke(context, new Object[]{"1", "2"}));
        assertEquals(3, method(context, "${Math.min}", Object.class, new Class<?>[]{int.class, int.class})
            .invoke(context, new Object[]{"5", "3"}));
        assertEquals(2L, method(context, "${Math.max}", Object.class, new Class<?>[]{long.class, long.class})
            .invoke(context, new Object[]{"1", "2"}));
    }

    @Test
    void aNullArgumentSelectsTheSameOverloadAsTheCompiledExpression() {
        ELContext context = new CompiledELContext().setBean("selection", new ParitySelection());
        assertEquals("int", value(context, "${selection.nullable(null)}"));
    }

    @Test
    void formattingExpressions() {
        ELContext context = new CompiledELContext().setBean("f", new ParityFormatting());
        assertEquals("0.50", value(context, "${f.format('%.2f', 0.5)}", String.class));
        assertEquals("a-b-c", value(context, "${f.join('-', 'a', 'b', 'c')}", String.class));
        assertEquals("only:-", value(context, "${f.join('-')}", String.class));
    }

    @Test
    void factoryExpressions() {
        ELContext context = new CompiledELContext()
            .setBean("suit", Suit.SPADE)
            .setBean("customer", "Guy Lafleur");
        assertEquals(Boolean.TRUE, value(context, "${suit == 'SPADE'}", Boolean.class));
        assertEquals("Welcome Guy Lafleur to our site", value(context, "Welcome ${customer} to our site", String.class));
    }

    @Test
    void lvalueExpressions() {
        Book book = new Book("EL", "history", 20d);
        ELContext context = new CompiledELContext()
            .setBean("book", book)
            .setBean("author", new ParityAuthor("Denis", List.of(book)));

        ValueExpression category = expression(context, "${book.category}", String.class);
        assertTrue(category.isReadOnly(context));
        assertThrows(PropertyNotWritableException.class, () -> category.setValue(context, "computing"));
        assertEquals("Hi, Denis", method(context, "${author.greet}", String.class, new Class<?>[]{String.class})
            .invoke(context, new Object[]{"Hi"}));
    }
}
