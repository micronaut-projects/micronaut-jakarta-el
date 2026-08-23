package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

/**
 * The lambda expressions: compiled to Java lambdas where they are consumed in place, to compiled
 * {@code LambdaExpression}s where they are values.
 */
@ELEnvironment(variables = @ELVariable(name = "item", type = Inventory.class))
@ELExpression(value = "${item.tags.stream().filter(t -> t.length() > 1).map(t -> t += '!').toList()}", name = "streamPipeline")
@ELExpression(value = "${item.tags.stream().sorted((a, b) -> b.length() - a.length()).toList()}", name = "streamSorted")
@ELExpression(value = "${item.tags.stream().anyMatch(t -> t == 'b').get()}", name = "streamAnyMatch")
@ELExpression(value = "${item.tags.stream().reduce('', (a, b) -> a += b)}", name = "streamReduce")
@ELExpression(value = "${item.tags.stream().filter(t -> t == 'none').findFirst().orElseGet(() -> 'fallback')}", name = "optionalSupplier")
@ELExpression(value = "${item.tags.stream().forEach(t -> t.length())}", name = "streamForEach")
@ELExpression(value = "${item.count(t -> t.length() > 1)}", name = "functionalInterface")
@ELExpression(value = "${item.describe(i -> i.sku += ':' += i.quantity)}", name = "functionalInterfaceTyped")
@ELExpression(value = "${item.adjusted((p, q) -> p * q + 1)}", name = "customFunctionalInterface")
@ELExpression(value = "${item.note.map(n -> n.length()).orElse(0)}", name = "optionalMap")
@ELExpression(value = "${(x -> x * 2)(item.quantity)}", name = "immediateInvocation")
@ELExpression(value = "${f = (x, y) -> x + y; f(1, 2)}", name = "lambdaVariable")
@ELExpression(value = "${(x -> y -> x + y)(1)(2)}", name = "nestedLambda")
@ELExpression(value = "${((a, b, c, d) -> a + b + c + d)(1, 2, 3, 4)}", name = "fourParameters")
@ELExpression(value = "${(() -> item.quantity)()}", name = "noParameters")
@ELExpression(value = "${((x, y) -> x + y)(1)}", name = "missingArgument")
@ELExpression(value = "${[3, 1, 2].stream().filter(n -> n > 1).map(n -> n * 2).toList()}", name = "dynamicStream")
public final class LambdaExpressions {

    private LambdaExpressions() {
    }
}
