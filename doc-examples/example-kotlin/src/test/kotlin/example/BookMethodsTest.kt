package example

import io.micronaut.el.CompiledELContext
import jakarta.el.ELManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BookMethodsTest {

    private val factory = ELManager.getExpressionFactory()
    private val context = CompiledELContext().setBean("book", Book("Jakarta EL", "reference", 20.0))

    private fun evaluate(expression: String): Any? =
        factory.createValueExpression(context, expression, Any::class.java).getValue(context)

    @Test
    fun theContributedMethodsAreCallableFromAnExpressionParsedAtRuntime() {
        assertEquals("Jakarta EL", evaluate("\${book.title()}"))
        assertEquals(18.0, evaluate("\${book.discounted(10)}"))
        assertEquals("\$18.0", evaluate("\${book.label('\$', 10)}"))
        assertEquals("Jakarta EL a-b", evaluate("\${book.tagged('-', 'a', 'b')}"))
        assertEquals(7L, evaluate("\${Math.abs(-7)}"))
        assertEquals("HI!", evaluate("\${fmt:shout('hi')}"))
    }

    @Test
    fun aLambdaReachesTheApplicationInterfaceWithoutAProxy() {
        assertEquals("Jakarta EL", evaluate("\${book.summarised(b -> b.title())}"))
    }
}
