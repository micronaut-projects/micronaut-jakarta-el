package example

import io.micronaut.el.CompiledELContext
import io.micronaut.el.ELSandbox
import io.micronaut.el.ELSandboxException
import jakarta.el.ELManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class SandboxTest {

    @Test
    fun theSandboxCanBeWidened() {
        val context = CompiledELContext().setBean("book", Book("Jakarta EL", "reference", 20.0))
        val expression = ELManager.getExpressionFactory()
            .createValueExpression(context, "\${book.class.name}", String::class.java) // <1>
        assertThrows(ELSandboxException::class.java) { expression.getValue(context) } // <2>

        context.putContext(ELSandbox::class.java, ELSandbox.UNRESTRICTED) // <3>
        assertEquals("example.Book", expression.getValue(context))
    }
}
