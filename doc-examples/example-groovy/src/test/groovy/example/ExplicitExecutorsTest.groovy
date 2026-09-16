package example

import io.micronaut.el.CompiledELContext
import io.micronaut.el.CompiledExpressionFactory
import io.micronaut.el.ContributedELMethodExecutor
import io.micronaut.el.interpreter.InterpretingELExpressionParser
import jakarta.el.ELContext
import jakarta.el.ExpressionFactory
import jakarta.el.MethodNotFoundException
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows

class ExplicitExecutorsTest {

    @Test
    void onlyTheContributedMethodsAreReachable() {
        ExpressionFactory factory = new CompiledExpressionFactory([], // <1>
            new InterpretingELExpressionParser([new ContributedELMethodExecutor([new BookMethods()])])) // <2>
        ELContext context = new CompiledELContext().setBean("book", new Book("Jakarta EL", "reference", 20d))

        assertEquals("Jakarta EL", factory.createValueExpression(context, '${book.title()}', Object).getValue(context)) // <3>
        assertThrows(MethodNotFoundException) {
            factory.createValueExpression(context, '${book.getTitle()}', Object).getValue(context) // <4>
        }
    }
}
