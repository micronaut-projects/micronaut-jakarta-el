package example

import io.micronaut.el.CompiledELContext
import io.micronaut.el.CompiledExpressionFactory
import io.micronaut.el.ContributedELMethodExecutor
import io.micronaut.el.interpreter.InterpretingELExpressionParser
import jakarta.el.MethodNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ExplicitExecutorsTest {

    @Test
    fun onlyTheContributedMethodsAreReachable() {
        val factory = CompiledExpressionFactory(listOf(), // <1>
            InterpretingELExpressionParser(listOf(ContributedELMethodExecutor(listOf(BookMethods()))))) // <2>
        val context = CompiledELContext().setBean("book", Book("Jakarta EL", "reference", 20.0))

        assertEquals("Jakarta EL", factory.createValueExpression(context, "\${book.title()}", Any::class.java).getValue(context)) // <3>
        assertThrows(MethodNotFoundException::class.java) {
            factory.createValueExpression(context, "\${book.getTitle()}", Any::class.java).getValue(context) // <4>
        }
    }
}
