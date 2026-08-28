package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImportShadowExpressionsTest {

    @Test
    void aCreationTimeVariableBindingShadowsAnImportedClass() {
        CompiledELContext context = new CompiledELContext();
        ExpressionFactory factory = new CompiledExpressionFactory(
            List.of(new ImportShadowExpressions$ELExpressions()));
        context.getVariableMapper().setVariable("Integer",
            factory.createValueExpression(new ImportShadow(), ImportShadow.class));

        ValueExpression field = factory.createValueExpression(context, "${Integer.MAX_VALUE}", String.class);
        ValueExpression method = factory.createValueExpression(context, "${Integer.valueOf('1')}", String.class);

        assertEquals("variable", field.getValue(context));
        assertEquals("variable:1", method.getValue(context));
    }
}
