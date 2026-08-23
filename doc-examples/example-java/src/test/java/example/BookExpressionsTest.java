package example;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.runtime.CompiledExpression;
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.ELManager;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookExpressionsTest {

    @Test
    void evaluatesTheCompiledExpressions() {
        ELContext context = new CompiledELContext() // <1>
            .setBean("book", new Book("Jakarta EL", "reference", 20d));
        ExpressionFactory factory = ELManager.getExpressionFactory();

        ValueExpression title = factory.createValueExpression(context, "${book.title}", String.class); // <2>
        assertTrue(title instanceof CompiledExpression); // <3>
        assertEquals("Jakarta EL", title.getValue(context));
        assertEquals("Book: Jakarta EL at 20.0",
            factory.createValueExpression(context, "Book: ${book.title} at ${book.unitPrice}", String.class).getValue(context));
        assertEquals(18d,
            factory.createMethodExpression(context, "${book.discounted(10)}", double.class, new Class<?>[0]).invoke(context, null)); // <4>

        assertEquals("expensive", BookExpressions$ELExpressions.PRICE_BAND.getValue(context)); // <5>
    }
}
