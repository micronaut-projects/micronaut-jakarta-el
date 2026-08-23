package example

import io.micronaut.el.CompiledELContext
import io.micronaut.el.runtime.CompiledExpression
import jakarta.el.ELManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class RuntimeExpressionTest {

    @Test
    fun parsesAnExpressionBuiltAtRuntime() {
        val context = CompiledELContext().setBean("book", Book("Jakarta EL", "reference", 20.0))
        val property = "category" // <1>

        val expression = ELManager.getExpressionFactory()
            .createValueExpression(context, "\${book.$property}", String::class.java) // <2>

        assertFalse(expression is CompiledExpression) // <3>
        assertEquals("reference", expression.getValue(context))
    }
}
