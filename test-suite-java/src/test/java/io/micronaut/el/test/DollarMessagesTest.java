package io.micronaut.el.test;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.runtime.CompiledExpression;
import io.micronaut.inject.ExecutableMethod;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The same patterns with {@code ${...}}, which Micronaut reserves for property placeholders.
 */
class DollarMessagesTest {

    @Test
    void anIntrospectionKeepsTheTextAndTheExpressionsAreCompiled() {
        // an introspection is not environment aware: nothing resolves placeholders in its metadata
        BeanIntrospection<OrderDollar> introspection = BeanIntrospection.getIntrospection(OrderDollar.class);
        AnnotationMetadata code = introspection.getRequiredProperty("code", Object.class).getAnnotationMetadata();
        AnnotationMetadata amount = introspection.getRequiredProperty("amount", Object.class).getAnnotationMetadata();
        assertEquals("value ${validatedValue.toUpperCase()} must be at least {min}",
            code.getAnnotationValuesByType(Size.class).get(0).stringValue("message").orElseThrow());
        assertEquals("${formatter.format('%1$.2f', validatedValue)} must be larger than {value}",
            amount.getAnnotationValuesByType(Min.class).get(0).stringValue("message").orElseThrow());
        ELContext context = new CompiledELContext();
        assertTrue(ELManager.getExpressionFactory()
            .createValueExpression(context, "${validatedValue.toUpperCase()}", Object.class) instanceof CompiledExpression);
        assertTrue(ELManager.getExpressionFactory()
            .createValueExpression(context, "${formatter.format('%1$.2f', validatedValue)}", Object.class) instanceof CompiledExpression);
    }

    /**
     * On a bean, the environment aware metadata resolves {@code ${...}} as a property placeholder: only the
     * annotations of the method itself get that metadata, the parameters never do.
     */
    @Test
    void aBeanResolvesDollarAsAPropertyPlaceholderOnTheMethodButNotOnItsParameters() {
        try (ApplicationContext applicationContext = ApplicationContext.run()) {
            ExecutableMethod<OrderService, Object> save = applicationContext.getBeanDefinition(OrderService.class)
                .getRequiredMethod("save", String.class, String.class, String.class);

            // a parameter: the text survives, whichever delimiter, because arguments are not environment aware
            assertEquals("value ${validatedValue.toUpperCase()} must be at least {min}",
                constraint(save.getArguments()[0].getAnnotationMetadata()).stringValue("message").orElseThrow());
            assertEquals("value #{validatedValue.toUpperCase()} must be at least {min}",
                constraint(save.getArguments()[1].getAnnotationMetadata()).stringValue("message").orElseThrow());

            // the method, a return value constraint: ${...} is a property placeholder the environment cannot
            // resolve, and the message is unreadable before any interpolator sees it
            AnnotationMetadata method = save.getAnnotationMetadata();
            assertTrue(method.hasPropertyExpressions());
            ConfigurationException failure = assertThrows(ConfigurationException.class,
                () -> constraint(method).stringValue("message"));
            assertTrue(failure.getMessage().contains("Could not resolve placeholder ${validatedValue.toUpperCase()}"), failure.getMessage());
        }
    }

    private static AnnotationValue<Size> constraint(AnnotationMetadata metadata) {
        return metadata.getAnnotationValuesByType(Size.class).get(0);
    }
}
