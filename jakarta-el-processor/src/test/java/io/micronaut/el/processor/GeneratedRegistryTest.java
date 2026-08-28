package io.micronaut.el.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.ELExpressionSource;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Compiles the source with the processor and loads the generated registry, so that what it serves is what
     * the compiled classes do rather than what they look like.
     */
    private static Generated registry() throws Exception {
        List<JavaFileObject> generated;
        try (JavaParser parser = new JavaParser()) {
            generated = new ArrayList<>();
            parser.generate(PACKAGE + ".Expressions", SOURCE).forEach(generated::add);
        }
        Map<String, byte[]> classes = new HashMap<>();
        for (JavaFileObject file : generated) {
            String name = file.getName();
            if (!name.endsWith(".class")) {
                continue;
            }
            try (InputStream input = file.openInputStream()) {
                classes.put(className(name), input.readAllBytes());
            }
        }
        ClassLoader loader = new ClassLoader(GeneratedRegistryTest.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = classes.get(name);
                if (bytes == null) {
                    throw new ClassNotFoundException(name);
                }
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
        return new Generated(loader,
            (ELExpressionSource) loader.loadClass(PACKAGE + ".Expressions$ELExpressions")
                .getDeclaredConstructor()
                .newInstance());
    }

    /**
     * The registry and the loader it was defined by, which also defines the types the expressions were
     * compiled against and therefore the beans they can be evaluated with.
     *
     * @param loader   The loader
     * @param registry The registry
     */
    private record Generated(ClassLoader loader, ELExpressionSource registry) {

        Object book() throws Exception {
            return loader.loadClass(PACKAGE + ".Expressions$Book").getDeclaredConstructor().newInstance();
        }
    }

    private static String className(String fileName) {
        String name = fileName.replace('\\', '/');
        int start = name.indexOf(PACKAGE.replace('.', '/'));
        return name.substring(start, name.length() - ".class".length()).replace('/', '.');
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
        Generated generated = registry();
        ELExpressionSource registry = generated.registry();
        CompiledELContext context = new CompiledELContext()
            .setBean("book", generated.book())
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
        Generated generated = registry();
        ELExpressionSource registry = generated.registry();
        CompiledELContext context = new CompiledELContext().setBean("book", generated.book());

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
