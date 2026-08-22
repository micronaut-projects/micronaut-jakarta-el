package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CompiledExpressionsTest {

    @Test
    void compiledValueExpressions() {
        CompiledELContext context = new CompiledELContext().setBean("book", new Book("EL", "history", 20d));
        assertEquals("EL", BookExpressions$ELExpressions.TITLE.getValue(context));
        assertEquals("Book: EL costs 20.0", BookExpressions$ELExpressions.SUMMARY.getValue(context));
        assertEquals(Double.valueOf(18d), BookExpressions$ELExpressions.DISCOUNTED.getValue(context));
    }
}
