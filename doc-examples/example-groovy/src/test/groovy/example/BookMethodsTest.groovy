package example

import io.micronaut.el.CompiledELContext
import jakarta.el.ELContext
import jakarta.el.ELManager
import jakarta.el.ExpressionFactory
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals

class BookMethodsTest {

    private final ExpressionFactory factory = ELManager.expressionFactory
    private final ELContext context = new CompiledELContext()
        .setBean("book", new Book("Jakarta EL", "reference", 20d))

    private Object evaluate(String expression) {
        factory.createValueExpression(context, expression, Object).getValue(context)
    }

    @Test
    void theContributedMethodsAreCallableFromAnExpressionParsedAtRuntime() {
        assertEquals("Jakarta EL", evaluate('${book.title()}'))
        assertEquals(18d, evaluate('${book.discounted(10)}'))
        assertEquals('$18.0', evaluate('${book.label(\'$\', 10)}'))
        assertEquals("Jakarta EL a-b", evaluate('${book.tagged(\'-\', \'a\', \'b\')}'))
        assertEquals(7L, evaluate('${Math.abs(-7)}'))
        assertEquals("HI!", evaluate('${fmt:shout(\'hi\')}'))
    }

    @Test
    void aLambdaReachesTheApplicationInterfaceWithoutAProxy() {
        assertEquals("Jakarta EL", evaluate('${book.summarised(b -> b.title())}'))
    }
}
