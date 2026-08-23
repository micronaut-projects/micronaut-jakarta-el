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
    void evaluatesTheOperatorsInlinedFromThePrimitiveProperties() {
        assertEquals(41d, factory.createValueExpression(context, '${book.unitPrice * 2 + 1}', Object).getValue(context))
        assertEquals(-20d, factory.createValueExpression(context, '${-book.unitPrice}', Object).getValue(context))
        assertEquals(true, factory.createValueExpression(context,
            '${not (book.unitPrice > 15) or book.title == "Expression Language"}', Object).getValue(context))
        assertEquals("Expression Language: 20.0",
            factory.createValueExpression(context, '${book.title += ": " += book.unitPrice}', Object).getValue(context))
    }

    @Test
    void evaluatesTheLambdaExpressionsCompiledToJavaLambdas() {
        assertEquals(["new!", "sale!"], factory.createValueExpression(context,
            '${book.tags.stream().filter(t -> t.length() > 1).map(t -> t += "!").toList()}', Object).getValue(context))
        assertEquals(2L, factory.createValueExpression(context, '${book.count(t -> t.length() > 1)}', Object).getValue(context))
        assertEquals(3L, factory.createValueExpression(context, '${(x -> y -> x + y)(1)(2)}', Object).getValue(context))
        assertEquals(10L, factory.createValueExpression(context, '${((a, b, c, d) -> a + b + c + d)(1, 2, 3, 4)}', Object).getValue(context))
        assertEquals(null, factory.createValueExpression(context, '${book.tags.stream().forEach(t -> t.length())}', Object).getValue(context))
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
