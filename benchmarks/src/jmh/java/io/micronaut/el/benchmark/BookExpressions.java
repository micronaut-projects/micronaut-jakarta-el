package io.micronaut.el.benchmark;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

/**
 * The benchmarked expressions, compiled at compilation time for the {@code compiled} stack. The other stacks
 * parse the same strings at runtime.
 */
@ELEnvironment(variables = @ELVariable(name = "book", type = Book.class))
@ELExpression(value = EvaluationBenchmark.PROPERTY, expectedType = String.class, name = "property")
@ELExpression(value = EvaluationBenchmark.NESTED_PROPERTY, expectedType = String.class, name = "nestedProperty")
@ELExpression(value = EvaluationBenchmark.COMPOSITE, expectedType = String.class, name = "composite")
@ELExpression(value = EvaluationBenchmark.ARITHMETIC, expectedType = String.class, name = "arithmetic")
@ELExpression(value = EvaluationBenchmark.COMPARISON, expectedType = Boolean.class, name = "comparison")
@ELExpression(value = EvaluationBenchmark.METHOD_CALL, expectedType = Double.class, name = "methodCall")
@ELExpression(value = EvaluationBenchmark.MAP_ACCESS, expectedType = String.class, name = "mapAccess")
@ELExpression(value = EvaluationBenchmark.STREAM, expectedType = Object.class, name = "stream")
@ELExpression(value = EvaluationBenchmark.LAMBDA, expectedType = Object.class, name = "lambda")
@ELExpression(value = EvaluationBenchmark.MATH, expectedType = Object.class, name = "math")
@ELExpression(value = EvaluationBenchmark.COMPLEX, expectedType = String.class, name = "complex")
@ELExpression(value = EvaluationBenchmark.LIST_INDEX, expectedType = String.class, name = "listIndex")
@ELExpression(value = EvaluationBenchmark.STATIC_METHOD, expectedType = String.class, name = "staticMethod")
@ELExpression(value = EvaluationBenchmark.STRING_METHODS, expectedType = String.class, name = "stringMethods")
@ELExpression(value = EvaluationBenchmark.EMPTY_CHECK, expectedType = Boolean.class, name = "emptyCheck")
@ELExpression(value = EvaluationBenchmark.DYNAMIC_BEAN, expectedType = String.class, name = "dynamicBean")
@ELExpression(value = EvaluationBenchmark.CUSTOM_LAMBDA, expectedType = Double.class, name = "customLambda")
public final class BookExpressions {

    private BookExpressions() {
    }
}
