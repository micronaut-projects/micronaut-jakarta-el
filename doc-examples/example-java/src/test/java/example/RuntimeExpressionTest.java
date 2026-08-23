package example;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.runtime.CompiledExpression;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RuntimeExpressionTest {

    @Test
    void parsesAnExpressionBuiltAtRuntime() {
        ELContext context = new CompiledELContext().setBean("book", new Book("Jakarta EL", "reference", 20d));
        String property = "category"; // <1>

        ValueExpression expression = ELManager.getExpressionFactory()
            .createValueExpression(context, "${book." + property + "}", String.class); // <2>

        assertFalse(expression instanceof CompiledExpression); // <3>
        assertEquals("reference", expression.getValue(context));
    }
}
