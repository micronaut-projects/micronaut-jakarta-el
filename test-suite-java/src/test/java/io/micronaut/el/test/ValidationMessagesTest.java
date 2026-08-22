package io.micronaut.el.test;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.runtime.CompiledExpression;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The constraint messages are the patterns of the Jakarta Validation TCK with {@code #{...}} in place of
 * {@code ${...}}. Micronaut leaves them alone, the processor compiles their expressions, and an interpolator
 * following the algorithm of the specification resolves the parameters first and the expressions through the
 * factory.
 */
class ValidationMessagesTest {

    private final BeanIntrospection<Order> introspection = BeanIntrospection.getIntrospection(Order.class);
    private final ExpressionFactory factory = ELManager.getExpressionFactory();

    @Test
    void theMessagesSurviveUnchanged() {
        assertEquals("value #{validatedValue.toUpperCase()} must be at least {min}",
            constraint(property("code"), Size.class).stringValue("message").orElseThrow());
        assertEquals("#{formatter.format('%1$.2f', validatedValue)} must be larger than {value}",
            constraint(property("amount"), Min.class).stringValue("message").orElseThrow());
        assertThrows(ClassNotFoundException.class, () -> Class.forName("io.micronaut.el.test.$Order$Expr0"));
    }

    @Test
    void theExpressionsOfTheMessagesWereCompiled() {
        ELContext context = new CompiledELContext();
        assertTrue(factory.createValueExpression(context, "#{validatedValue.toUpperCase()}", Object.class)
            instanceof CompiledExpression);
        assertTrue(factory.createValueExpression(context, "#{formatter.format('%1$.2f', validatedValue)}", Object.class)
            instanceof CompiledExpression);
    }

    @Test
    void theMessagesInterpolateLikeTheSpecification() {
        assertEquals("value ABC must be at least 5",
            interpolate(property("code"), Size.class, "abc"));
        assertEquals("12.50 must be larger than 100",
            interpolate(property("amount"), Min.class, 12.5d));
    }

    private AnnotationMetadata property(String name) {
        return introspection.getRequiredProperty(name, Object.class).getAnnotationMetadata();
    }

    /** A constraint is repeatable, so it is read through its container. */
    private static <A extends java.lang.annotation.Annotation> AnnotationValue<A> constraint(AnnotationMetadata metadata, Class<A> type) {
        return metadata.getAnnotationValuesByType(type).get(0);
    }

    /** The algorithm of the section 6.3.1.1 of Jakarta Validation: parameters first, then the expressions. */
    private String interpolate(AnnotationMetadata metadata, Class<? extends java.lang.annotation.Annotation> constraint, Object validatedValue) {
        AnnotationValue<?> annotation = constraint(metadata, constraint);
        String template = annotation.stringValue("message").orElseThrow();
        Map<CharSequence, Object> attributes = annotation.getValues();
        StringBuilder parameters = new StringBuilder();
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '{' && (i == 0 || template.charAt(i - 1) != '#')) {
                int end = template.indexOf('}', i);
                Object attribute = attributes.get(template.substring(i + 1, end));
                parameters.append(attribute == null ? template.substring(i, end + 1) : attribute);
                i = end;
            } else {
                parameters.append(c);
            }
        }
        ELContext context = new CompiledELContext()
            .setBean("validatedValue", validatedValue)
            .setBean("formatter", new LocaleFormatter(Locale.US));
        String resolved = parameters.toString();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < resolved.length(); i++) {
            if (resolved.startsWith("#{", i)) {
                int end = resolved.indexOf('}', i);
                ValueExpression expression = factory.createValueExpression(context, resolved.substring(i, end + 1), Object.class);
                assertTrue(expression instanceof CompiledExpression, "not compiled: " + resolved.substring(i, end + 1));
                Object value = expression.getValue(context);
                result.append(value);
                i = end;
            } else {
                result.append(resolved.charAt(i));
            }
        }
        return result.toString();
    }
}
