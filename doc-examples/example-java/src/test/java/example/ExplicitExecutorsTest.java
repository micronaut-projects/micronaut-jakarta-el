package example;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.ContributedELMethodExecutor;
import io.micronaut.el.interpreter.InterpretingELExpressionParser;
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExplicitExecutorsTest {

    @Test
    void onlyTheContributedMethodsAreReachable() {
        ExpressionFactory factory = new CompiledExpressionFactory(List.of(), // <1>
            new InterpretingELExpressionParser(List.of(new ContributedELMethodExecutor(List.of(new BookMethods()))))); // <2>
        ELContext context = new CompiledELContext().setBean("book", new Book("Jakarta EL", "reference", 20d));

        assertEquals("Jakarta EL", factory.createValueExpression(context, "${book.title()}", Object.class).getValue(context)); // <3>
        assertThrows(MethodNotFoundException.class,
            () -> factory.createValueExpression(context, "${book.getTitle()}", Object.class).getValue(context)); // <4>
    }
}
