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

import jakarta.el.LambdaExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.micronaut.el.interpreter.parity.ParityEvaluation.value;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The interpreted counterparts of {@code io.micronaut.el.test.CatalogExpressions}, with the imports, static
 * imports and functions of its environment and the results {@code CatalogExpressionsTest} asserts.
 */
class ParityCatalogExpressionsTest {

    private final List<Book> books = List.of(
        new Book("Expression Language", "history", 20d),
        new Book("Sourcegen", "computing", 5d)
    );
    private final ParityAuthor author = new ParityAuthor("Denis", books);
    private final ParityEvaluation.FunctionContext context = context();

    private ParityEvaluation.FunctionContext context() {
        ParityEvaluation.FunctionContext context = new ParityEvaluation.FunctionContext();
        try {
            context.function("fn", "length", ParityTextFunctions.class.getMethod("length", String.class))
                .function("fn", "upper", ParityTextFunctions.class.getMethod("toUpperCase", String.class));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
        context.setBean("author", author)
            .setBean("books", books)
            .setBean("strings", new String[]{"a", "b"})
            .setBean("customer", author);
        context.getImportHandler().importClass(Suit.class.getName());
        context.getImportHandler().importClass(Book.class.getName());
        context.getImportHandler().importStatic(Suit.class.getName() + ".SPADE");
        return context;
    }

    @Test
    void staticallyTypedResolution() {
        assertEquals("Denis", value(context, "${author.name}"));
        assertEquals("Hi, Denis", value(context, "${author.greet('Hi')}"));
        assertEquals("Expression Language", value(context, "${author.books[0].title}"));
    }

    @Test
    void dynamicResolution() {
        assertEquals("Denis", value(context, "${customer.name}"));
        assertEquals("history", value(context, "${customer.books[0].category}"));
    }

    @Test
    void functions() {
        assertEquals(5, value(context, "${fn:length(author.name)}"));
        assertEquals("DENIS", value(context, "${fn:upper(author.name)}"));
    }

    @Test
    void staticReferencesAndConstructors() {
        assertEquals(Suit.SPADE, value(context, "${Suit.SPADE}"));
        assertEquals(Suit.SPADE, value(context, "${SPADE}"));
        assertEquals(42, value(context, "${Integer.valueOf('42')}"));
        assertEquals("EL", ((Book) value(context, "${Book('EL', 'history', 10)}")).getTitle());
    }

    @Test
    void collectionOperations() {
        assertEquals(List.of("Expression Language"),
            value(context, "${books.stream().filter(b->b.unitPrice ge 10).map(b->b.title).toList()}"));
        assertEquals(25d, value(context, "${books.stream().map(b->b.unitPrice).sum()}"));
        assertEquals("Expression Language",
            value(context, "${books.stream().max((p,q)->p.unitPrice-q.unitPrice).get().title}"));
    }

    @Test
    void aNestedLambdaValue() {
        LambdaExpression outer = (LambdaExpression) value(context, "${x->y->x+y}");
        LambdaExpression inner = (LambdaExpression) outer.invoke(context, 3L);
        assertEquals(7L, inner.invoke(context, 4L));
    }
}
