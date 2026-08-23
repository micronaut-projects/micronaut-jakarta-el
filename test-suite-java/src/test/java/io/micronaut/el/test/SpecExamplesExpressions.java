package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

import java.util.Map;

/**
 * Examples following the chapters of the Jakarta Expression Language specification, each compiled to a class of
 * its own. The numbers are the sections of the specification.
 */
@ELEnvironment(
    variables = {
        @ELVariable(name = "item", type = Inventory.class),
        @ELVariable(name = "prices", type = Map.class)
    },
    imports = Suit.class
)
// 1.2 literal expressions and composite expressions
@ELExpression(value = "plain text", expectedType = String.class, name = "literalText")
@ELExpression(value = "\\${not an expression}", expectedType = String.class, name = "escapedExpression")
@ELExpression(value = "${item.sku} has ${item.quantity} in ${item.suit}", expectedType = String.class, name = "compositeText")
// 1.4 literals
@ELExpression(value = "${42}", name = "integerLiteral")
@ELExpression(value = "${4.2}", name = "floatingPointLiteral")
@ELExpression(value = "${1e3}", name = "exponentLiteral")
@ELExpression(value = "${'it\\'s'}", name = "stringLiteralQuote")
@ELExpression(value = "${\"double \\\"quoted\\\"\"}", name = "stringLiteralDoubleQuoted")
@ELExpression(value = "${true}", name = "booleanLiteral")
@ELExpression(value = "${null}", name = "nullLiteral")
// 1.6 property and method access, 1.7 arithmetic, 1.8 string concatenation
@ELExpression(value = "${item['sku']}", name = "bracketProperty")
@ELExpression(value = "${item.tags[1]}", name = "listIndex")
@ELExpression(value = "${prices['widget']}", name = "mapKey")
@ELExpression(value = "${prices.widget}", name = "mapProperty")
@ELExpression(value = "${item.tags.size()}", name = "methodOnList")
@ELExpression(value = "${item.quantity * item.price - item.reorderLevel / 2}", name = "mixedArithmetic")
@ELExpression(value = "${item.quantity div 2}", name = "divWord")
@ELExpression(value = "${item.quantity mod 3}", name = "modWord")
@ELExpression(value = "${-(item.quantity + 1)}", name = "negatedParenthesis")
@ELExpression(value = "${'10' + 5}", name = "stringArithmetic")
@ELExpression(value = "${'10' += 5}", name = "stringConcatenationOperator")
@ELExpression(value = "${null + 1}", name = "nullArithmetic")
// 1.9 relational and equality, 1.10 logical, 1.11 empty, 1.12 conditional
@ELExpression(value = "${item.quantity eq 5}", name = "eqWord")
@ELExpression(value = "${item.quantity ne 5}", name = "neWord")
@ELExpression(value = "${item.sku lt 'B'}", name = "stringLessThan")
@ELExpression(value = "${item.suit == 'HEART'}", name = "enumEqualsString")
@ELExpression(value = "${item.suit == Suit.HEART}", name = "enumEqualsConstant")
@ELExpression(value = "${'5' == item.quantity}", name = "coercedEquality")
@ELExpression(value = "${item.available && not item.available || item.quantity > 0}", name = "logicalWords")
@ELExpression(value = "${empty item.sku}", name = "emptyString")
@ELExpression(value = "${empty item.tags}", name = "emptyList")
@ELExpression(value = "${empty prices}", name = "emptyMap")
@ELExpression(value = "${item.quantity > 3 ? 'many' : 'few'}", name = "conditional")
@ELExpression(value = "${item.available ? item.quantity : -1}", name = "conditionalMixedTypes")
// 1.13 assignment, 1.14 semicolon
@ELExpression(value = "${total = item.quantity * 2; total + 1}", name = "assignmentAndSemicolon")
// 1.21 collection construction, 1.22 string to type coercion, 1.24 static references
@ELExpression(value = "${[1, 2, 3]}", name = "listConstruction")
@ELExpression(value = "${{1, 1, 2}}", name = "setConstruction")
@ELExpression(value = "${{'one': 1, 'two': 2}}", name = "mapConstruction")
@ELExpression(value = "${[item.sku, item.quantity]}", name = "listOfValues")
@ELExpression(value = "${Suit.SPADE}", name = "staticField")
@ELExpression(value = "${Suit.valueOf('CLUB')}", name = "staticMethod")
@ELExpression(value = "${Integer.MAX_VALUE}", name = "javaLangStaticField")
@ELExpression(value = "${Math.max(item.quantity, 7)}", name = "javaLangStaticMethod")
// 2.3 streams and optionals
@ELExpression(value = "${item.tags.stream().sorted().toList()}", name = "streamSorted")
@ELExpression(value = "${item.tags.stream().map(t -> t.length()).sum()}", name = "streamSum")
@ELExpression(value = "${item.tags.stream().map(t -> t.length()).average().get()}", name = "streamAverage")
@ELExpression(value = "${item.tags.stream().distinct().count()}", name = "streamCount")
@ELExpression(value = "${item.tags.stream().limit(2).toList()}", name = "streamLimit")
@ELExpression(value = "${item.tags.stream().substream(1, 3).toList()}", name = "streamSubstream")
@ELExpression(value = "${item.tags.stream().findFirst().orElse('none')}", name = "optionalOrElse")
@ELExpression(value = "${item.tags.stream().allMatch(t -> t.length() > 0).get()}", name = "streamAllMatch")
@ELExpression(value = "${item.tags.stream().noneMatch(t -> t == 'x').get()}", name = "streamNoneMatch")
@ELExpression(value = "${item.tags.stream().min().get()}", name = "streamMin")
@ELExpression(value = "${item.tags.stream().reduce((a, b) -> a += ',' += b).get()}", name = "streamReduce")
@ELExpression(value = "${[3, 1, 2].stream().sorted((a, b) -> a - b).toList()}", name = "literalStreamSorted")
@ELExpression(value = "${[[1, 2], [3]].stream().flatMap(l -> l.stream()).toList()}", name = "streamFlatMap")
public final class SpecExamplesExpressions {

    private SpecExamplesExpressions() {
    }
}
