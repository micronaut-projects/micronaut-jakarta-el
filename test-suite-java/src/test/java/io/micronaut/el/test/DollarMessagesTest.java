package io.micronaut.el.test;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.runtime.CompiledExpression;
import io.micronaut.inject.ExecutableMethod;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.ValueExpression;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The same patterns written with {@code ${...}}, the delimiters of the Jakarta Validation specification, which
 * Micronaut reserves for property placeholders. The remapper stores them as {@code #{...}}, so nothing in
 * Micronaut resolves them, on any path.
 */
class DollarMessagesTest {

    private final ELContext context = new CompiledELContext();

    @Test
    void anIntrospectionStoresTheNormalizedTextAndTheExpressionsAreCompiled() {
        BeanIntrospection<OrderDollar> introspection = BeanIntrospection.getIntrospection(OrderDollar.class);
        AnnotationMetadata code = introspection.getRequiredProperty("code", Object.class).getAnnotationMetadata();
        AnnotationMetadata amount = introspection.getRequiredProperty("amount", Object.class).getAnnotationMetadata();
        assertEquals("value #{validatedValue.toUpperCase()} must be at least {min}",
            constraint(code, Size.class).stringValue("message").orElseThrow());
        assertEquals("#{formatter.format('%1$.2f', validatedValue)} must be larger than {value}",
            constraint(amount, Min.class).stringValue("message").orElseThrow());
        assertFalse(code.hasPropertyExpressions());
        assertCompiled("#{validatedValue.toUpperCase()}");
        assertCompiled("#{formatter.format('%1$.2f', validatedValue)}");
    }

    /**
     * The method of a bean is the path where {@code ${...}} would fail, as the method metadata of a bean
     * resolves property placeholders once the context is running. Normalized, it reads like any other.
     */
    @Test
    void aBeanMethodNoLongerResolvesTheMessageAsAPropertyPlaceholder() {
        try (ApplicationContext applicationContext = ApplicationContext.run()) {
            ExecutableMethod<OrderService, Object> save = applicationContext.getBeanDefinition(OrderService.class)
                .getRequiredMethod("save", String.class, String.class, String.class);

            AnnotationMetadata method = save.getAnnotationMetadata();
            assertFalse(method.hasPropertyExpressions());
            assertEquals("result #{validatedValue.toUpperCase()} must be at least {min}",
                constraint(method, Size.class).stringValue("message").orElseThrow());

            assertEquals("value #{validatedValue.toUpperCase()} must be at least {min}",
                constraint(save.getArguments()[0].getAnnotationMetadata(), Size.class).stringValue("message").orElseThrow());
            assertEquals("value #{validatedValue.toUpperCase()} must be at least {min}",
                constraint(save.getArguments()[1].getAnnotationMetadata(), Size.class).stringValue("message").orElseThrow());

            // a member the remapper does not own keeps its placeholder, and its flag
            AnnotationMetadata named = save.getArguments()[2].getAnnotationMetadata();
            assertTrue(named.hasPropertyExpressions());
        }
    }

    @Test
    void aCustomAnnotationWrittenWithDollarResolvesThroughTheFactory() {
        BeanIntrospection<AnnotatedCatalogDollar> introspection = BeanIntrospection.getIntrospection(AnnotatedCatalogDollar.class);
        String stored = introspection.stringValue(MyNewAnnotation.class).orElseThrow();
        assertEquals("#{ book.title += ' (' += book.category += ')' }", stored);
        ELContext books = new CompiledELContext().setBean("book", new Book("EL", "reference", 20d));
        ValueExpression expression = ELManager.getExpressionFactory().createValueExpression(books, stored, String.class);
        assertTrue(expression instanceof CompiledExpression);
        assertEquals("EL (reference)", expression.getValue(books));
    }

    private void assertCompiled(String expression) {
        assertTrue(ELManager.getExpressionFactory().createValueExpression(context, expression, Object.class)
            instanceof CompiledExpression, "not compiled: " + expression);
    }

    private static <A extends java.lang.annotation.Annotation> AnnotationValue<A> constraint(AnnotationMetadata metadata, Class<A> type) {
        return metadata.getAnnotationValuesByType(type).get(0);
    }
}
