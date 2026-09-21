package example;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.ELSandbox;
import io.micronaut.el.ELSandboxException;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SandboxTest {

    @Test
    void theSandboxCanBeWidened() {
        ELContext context = new CompiledELContext().setBean("book", new Book("Jakarta EL", "reference", 20d));
        ValueExpression expression = ELManager.getExpressionFactory()
            .createValueExpression(context, "${book.class.name}", String.class); // <1>
        assertThrows(ELSandboxException.class, () -> expression.getValue(context)); // <2>

        context.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED); // <3>
        assertEquals("example.Book", expression.getValue(context));
    }
}
