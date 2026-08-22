package io.micronaut.el.test

import io.micronaut.el.CompiledELContext
import io.micronaut.el.resolver.IntrospectionELResolver
import jakarta.el.ELContext
import jakarta.el.ELProcessor
import jakarta.el.ExpressionFactory
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

/**
 * Uses the module from Groovy. The bean is introspected at compilation time by Micronaut, so its properties and
 * its executable methods resolve without reflection; the expressions are parsed by the interpreter module.
 */
class BookExpressionsSpec {

    private final ExpressionFactory factory = ExpressionFactory.newInstance()
    private final ELContext context = new CompiledELContext()
        .setBean("book", new Book("Expression Language", "reference", 20d))

    @Test
    void evaluatesExpressionsOverAGroovyBean() {
        assertEquals("Expression Language", factory.createValueExpression(context, '${book.title}', String).getValue(context))
        assertEquals("Book: Expression Language at 20.0",
            factory.createValueExpression(context, 'Book: ${book.title} at ${book.unitPrice}', String).getValue(context))
        assertEquals(18d, factory.createValueExpression(context, '${book.discounted(10)}', Double).getValue(context))
        assertEquals("expensive",
            factory.createValueExpression(context, '${book.unitPrice > 15 ? "expensive" : "cheap"}', String).getValue(context))
    }

    @Test
    void resolvesTheIntrospectedGroovyBeanWithoutReflection() {
        def resolver = new IntrospectionELResolver()
        context.setPropertyResolved(false)
        assertEquals("Expression Language", resolver.getValue(context, context.getBean("book"), "title"))
        assertTrue(context.isPropertyResolved())
    }

    @Test
    void supportsTheCollectionOperationsOfTheSpecification() {
        def processor = new ELProcessor()
        processor.defineBean("books", [
            new Book("a", "history", 30d),
            new Book("b", "history", 10d),
            new Book("c", "science", 20d)
        ])
        assertEquals(["a", "c"], processor.eval('books.stream().filter(b -> b.unitPrice >= 20).map(b -> b.title).toList()'))
        assertEquals(60d, processor.eval('books.stream().map(b -> b.unitPrice).sum()'))
    }
}
