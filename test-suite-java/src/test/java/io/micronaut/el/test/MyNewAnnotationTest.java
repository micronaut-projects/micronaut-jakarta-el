package io.micronaut.el.test;

import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.runtime.CompiledExpression;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyNewAnnotationTest {

    private final BeanIntrospection<AnnotatedCatalog> introspection = BeanIntrospection.getIntrospection(AnnotatedCatalog.class);
    private final ELContext context = new CompiledELContext().setBean("book", new Book("EL", "reference", 20d));

    @Test
    void micronautLeftTheExpressionAlone() {
        // the member holds the text as written: a plain string, no evaluated expression reference
        assertEquals("#{ book.title += ' (' += book.category += ')' }",
            introspection.stringValue(MyNewAnnotation.class).orElseThrow());
        // and Micronaut generated no expression class of its own for it
        assertThrows(ClassNotFoundException.class,
            () -> Class.forName("io.micronaut.el.test.$AnnotatedCatalog$Expr0"));
        assertFalse(introspection.getAnnotationMetadata().hasEvaluatedExpressions()
            && introspection.getValue(MyNewAnnotation.class, Object.class).orElse(null) instanceof io.micronaut.core.expressions.EvaluatedExpression);
    }

    @Test
    void theExpressionWasCompiledIntoTheRegistry() {
        ValueExpression expression = ELManager.getExpressionFactory()
            .createValueExpression(context, introspection.stringValue(MyNewAnnotation.class).orElseThrow(), String.class);
        assertTrue(expression instanceof CompiledExpression, "expected a compiled expression, got " + expression.getClass());
    }

    @Test
    void theValueIsResolvedWithTheExpressionEngine() {
        // the value is the expression as written, the factory returns the class compiled for it
        assertEquals("EL (reference)", ELManager.getExpressionFactory()
            .createValueExpression(context, introspection.stringValue(MyNewAnnotation.class).orElseThrow(), String.class)
            .getValue(context));
    }
}
