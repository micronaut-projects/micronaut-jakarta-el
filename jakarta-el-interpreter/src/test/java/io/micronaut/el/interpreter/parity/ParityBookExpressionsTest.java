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

import static io.micronaut.el.interpreter.parity.ParityEvaluation.value;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The interpreted counterparts of the {@code BookExpressions} of the Java, Groovy and Kotlin test suites. The
 * Groovy declarations quote their strings with double quotes and the Kotlin ones with single quotes, so both
 * spellings are evaluated.
 */
class ParityBookExpressionsTest {

    private final ELContext java = new CompiledELContext().setBean("book", new Book("EL", "history", 20d));
    private final ELContext context = new CompiledELContext()
        .setBean("book", new Book("Expression Language", "reference", 20d));

    @Test
    void theJavaSuite() {
        // CompiledExpressionsTest
        assertEquals("EL", value(java, "${book.title}", String.class));
        assertEquals("Book: EL costs 20.0", value(java, "Book: ${book.title} costs ${book.unitPrice}", String.class));
        assertEquals(18d, value(java, "${book.discounted(10)}", Double.class));
    }

    @Test
    void theExpressionsOfTheGroovyAndKotlinSuites() {
        assertEquals("Expression Language", value(context, "${book.title}", String.class));
        assertEquals("Book: Expression Language at 20.0",
            value(context, "Book: ${book.title} at ${book.unitPrice}", String.class));
        assertEquals(18d, value(context, "${book.discounted(10)}", Double.class));
    }

    @Test
    void theGroovySpellings() {
        assertEquals("expensive", value(context, "${book.unitPrice > 15 ? \"expensive\" : \"cheap\"}", String.class));
        assertEquals(true, value(context, "${not (book.unitPrice > 15) or book.title == \"Expression Language\"}"));
        assertEquals("Expression Language: 20.0", value(context, "${book.title += \": \" += book.unitPrice}"));
        assertEquals(List.of("new!", "sale!"),
            value(context, "${book.tags.stream().filter(t -> t.length() > 1).map(t -> t += \"!\").toList()}"));
    }

    @Test
    void theKotlinSpellings() {
        assertEquals("expensive", value(context, "${book.unitPrice > 15 ? 'expensive' : 'cheap'}", String.class));
        assertEquals(true, value(context, "${not (book.unitPrice > 15) or book.title == 'Expression Language'}"));
        assertEquals("Expression Language: 20.0", value(context, "${book.title += ': ' += book.unitPrice}"));
        assertEquals(List.of("new!", "sale!"),
            value(context, "${book.tags.stream().filter(t -> t.length() > 1).map(t -> t += '!').toList()}"));
    }

    @Test
    void theOperatorsAndLambdasSharedByTheGroovyAndKotlinSuites() {
        assertEquals(41d, value(context, "${book.unitPrice * 2 + 1}"));
        assertEquals(-20d, value(context, "${-book.unitPrice}"));
        assertEquals(2L, value(context, "${book.count(t -> t.length() > 1)}"));
        assertEquals(3L, value(context, "${(x -> y -> x + y)(1)(2)}"));
        assertEquals(10L, value(context, "${((a, b, c, d) -> a + b + c + d)(1, 2, 3, 4)}"));
        assertNull(value(context, "${book.tags.stream().forEach(t -> t.length())}"));
        assertEquals(3L, value(context, "${book.count(t -> [t].stream().allMatch(x -> x.length() > 0).get())}"));
    }
}
