package io.micronaut.el.processor;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.ELExpressionSource;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The registry the processor generates for the expressions of a class: what it serves for the expression
 * strings and the result types a caller asks for.
 */
class GeneratedRegistryTest {

    private static final String PACKAGE = "io.micronaut.el.test.registry";

    private static final String SOURCE = """
        package io.micronaut.el.test.registry;

        import io.micronaut.el.annotation.*;

        @ELEnvironment(variables = {
            @ELVariable(name = "book", type = Expressions.Book.class),
            @ELVariable(name = "quantity", type = int.class),
            @ELVariable(name = "tags", type = String[].class)
        })
        @ELExpression(value = "${book.title}", expectedType = String.class, name = "title")
        @ELExpression(value = "${quantity * 2}", expectedType = Integer.class, name = "twice")
        @ELExpression(value = "${tags[0]}", expectedType = String.class, name = "firstTag")
        @ELMethodExpression(value = "#{book.describe}", expectedReturnType = String.class, name = "describe")
        @ELMethodExpression(value = "#{book.discount}", expectedReturnType = Double.class,
            expectedParamTypes = Double.class, name = "discount")
        @ELMethodExpression(value = "#{book.discount(50)}", expectedReturnType = Double.class,
            name = "halfPrice")
        public class Expressions {

            public static class Book {
                public String getTitle() { return "EL"; }
                public String describe() { return "described"; }
                public double discount(Double percent) { return percent; }
            }
        }
        """;

    private static GeneratedExpressions registry() throws Exception {
        return GeneratedExpressions.of(PACKAGE + ".Expressions", SOURCE);
    }

    private static Object book(GeneratedExpressions generated) throws Exception {
        return generated.instantiate(PACKAGE + ".Expressions$Book");
    }

    @Test
    void theRegistryDeclaresEveryExpressionItWasGeneratedFor() throws Exception {
        assertEquals(
            List.of("${book.title}", "${quantity * 2}", "${tags[0]}",
                "#{book.describe}", "#{book.discount}", "#{book.discount(50)}"),
            registry().registry().expressions());
    }

    @Test
    void theRegistryServesTheValueExpressionsOfTheDeclaredTypes() throws Exception {
        GeneratedExpressions generated = registry();
        ELExpressionSource registry = generated.registry();
        CompiledELContext context = new CompiledELContext()
            .setBean("book", book(generated))
            .setBean("quantity", 3)
            .setBean("tags", new String[]{"first", "second"});

        ValueExpression title = registry.createValueExpression(context, "${book.title}", String.class);
        assertNotNull(title);
        assertEquals("EL", title.getValue(context));

        ValueExpression twice = registry.createValueExpression(context, "${quantity * 2}", Integer.class);
        assertNotNull(twice);
        assertEquals(Integer.valueOf(6), twice.getValue(context));

        ValueExpression firstTag = registry.createValueExpression(context, "${tags[0]}", String.class);
        assertNotNull(firstTag);
        assertEquals("first", firstTag.getValue(context));

        // a type the registry was not generated for is left to the caller to parse
        assertNull(registry.createValueExpression(context, "${book.title}", Integer.class));
        assertNull(registry.createValueExpression(context, "${nothing}", String.class));
    }

    @Test
    void theRegistryServesTheMethodExpressionsOfTheDeclaredSignatures() throws Exception {
        GeneratedExpressions generated = registry();
        ELExpressionSource registry = generated.registry();
        CompiledELContext context = new CompiledELContext().setBean("book", book(generated));

        MethodExpression describe = registry.createMethodExpression(context, "#{book.describe}",
            String.class, new Class<?>[0]);
        assertNotNull(describe);
        assertEquals("described", describe.invoke(context, null));

        MethodExpression discount = registry.createMethodExpression(context, "#{book.discount}",
            Double.class, new Class<?>[]{Double.class});
        assertNotNull(discount);
        assertEquals(25d, discount.invoke(context, new Object[]{25d}));

        MethodExpression halfPrice = registry.createMethodExpression(context, "#{book.discount(50)}",
            Double.class, null);
        assertNotNull(halfPrice);
        assertTrue(halfPrice.isParametersProvided());
        assertEquals(50d, halfPrice.invoke(context, null));

        // a caller that does not ask for a return type gets the result of the invocation uncoerced
        MethodExpression uncoerced = registry.createMethodExpression(context, "#{book.describe}",
            null, new Class<?>[0]);
        assertNotNull(uncoerced);
        assertEquals("described", uncoerced.invoke(context, null));

        // and the parameter types the expression was not generated for are not served
        assertNull(registry.createMethodExpression(context, "#{book.discount}", Double.class,
            new Class<?>[]{String.class}));
    }
}
