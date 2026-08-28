package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

@ELEnvironment(variables = @ELVariable(name = "Integer", type = ImportShadow.class))
@ELExpression(value = "${Integer.MAX_VALUE}", expectedType = String.class, name = "field")
@ELExpression(value = "${Integer.valueOf('1')}", expectedType = String.class, name = "method")
public final class ImportShadowExpressions {

    private ImportShadowExpressions() {
    }
}
