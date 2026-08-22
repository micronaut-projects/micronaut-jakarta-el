package io.micronaut.el.test

import io.micronaut.el.CompiledELContext
import io.micronaut.el.resolver.IntrospectionELResolver
import io.micronaut.el.runtime.CompiledExpression
import jakarta.el.ELContext
import jakarta.el.ELException
import jakarta.el.ExpressionFactory
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertThrows
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * The expressions of this suite are declared in Groovy source and compiled by the annotation processor. The
 * interpreter is deliberately absent from the classpath, so an expression that reaches the runtime unparsed
 * fails rather than falling back.
 */
class BookExpressionsSpec {

    private final ExpressionFactory factory = ExpressionFactory.newInstance()
    private final ELContext context = new CompiledELContext()
        .setBean("book", new Book("Expression Language", "reference", 20d))

    @Test
    void evaluatesTheExpressionsCompiledFromGroovySource() {
        assertEquals("Expression Language", factory.createValueExpression(context, '${book.title}', String).getValue(context))
        assertEquals("Book: Expression Language at 20.0",
            factory.createValueExpression(context, 'Book: ${book.title} at ${book.unitPrice}', String).getValue(context))
        assertEquals(18d, factory.createValueExpression(context, '${book.discounted(10)}', Double).getValue(context))
        assertEquals("expensive",
            factory.createValueExpression(context, '${book.unitPrice > 15 ? "expensive" : "cheap"}', String).getValue(context))
    }

    @Test
    void theExpressionsAreCompiledRatherThanParsed() {
        assertTrue(factory.createValueExpression(context, '${book.title}', String) instanceof CompiledExpression)
    }

    @Test
    void anUndeclaredExpressionIsRejectedWithoutTheInterpreter() {
        assertThrows(ELException) {
            factory.createValueExpression(context, '${book.category}', String)
        }
    }

    @Test
    void resolvesTheIntrospectedGroovyBeanWithoutReflection() {
        def resolver = new IntrospectionELResolver()
        context.setPropertyResolved(false)
        assertEquals("Expression Language", resolver.getValue(context, context.getBean("book"), "title"))
        assertTrue(context.isPropertyResolved())
    }
}
