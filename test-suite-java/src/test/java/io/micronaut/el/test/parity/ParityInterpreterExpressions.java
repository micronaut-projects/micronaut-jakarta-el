package io.micronaut.el.test.parity;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELVariable;

/**
 * Compile-time counterparts of the regressions of {@code ELInterpreterTest} and
 * {@code InterpretingELExpressionParserTest} that {@code io.micronaut.el.test.ELInterpreterExpressions} does not
 * declare yet.
 *
 * <p>Left out, and why:</p>
 * <ul>
 *     <li>the boolean relational rule ({@code ${false gt '9'}} and the rest of
 *     {@code aBooleanOperandDecidesARelationalComparison}) and the unary minus of a character
 *     ({@code ${-Character.valueOf(49)}}): their behaviour is being changed, and the change adds the pairs;</li>
 *     <li>{@code ${fn:combine()}}, {@code ${missing:call()}}, {@code ${varargs.route(value -> value)}},
 *     {@code ${ambiguous('x')}} and {@code Hello ${book.describe}} as a method expression: each is a
 *     compile-time rejection (too few fixed arguments, an undeclared prefix, ambiguous functional targets, a
 *     provably ambiguous static selection, a composite method expression), so no generated expression can exist,
 *     and {@code ELInterpreterTest} holds the interpreted runtime contract;</li>
 *     <li>{@code "${" + i + "}"} of the parser cache regression is a concatenation building expression strings at
 *     runtime, not an expression.</li>
 * </ul>
 */
@ELEnvironment(variables = @ELVariable(name = "bean", type = ParityHolder.class))
// anOverloadIsSelectedTheWayTheSpecificationSelectsIt
@ELExpression(value = "${Integer.valueOf(1)}", name = "integerValueOf")
@ELExpression(value = "${Long.valueOf(1)}", name = "longValueOf")
@ELExpression(value = "${Double.valueOf(1)}", name = "doubleValueOf")
@ELExpression(value = "${String.valueOf(1)}", name = "stringValueOf")
@ELExpression(value = "${Character.valueOf(65)}", name = "characterValueOf")
@ELExpression(value = "${Boolean.valueOf(1)}", name = "booleanValueOfNumber")
@ELExpression(value = "${Boolean.valueOf('true')}", name = "booleanValueOfString")
// aStreamHoldingANullElementDoesNotFail
@ELExpression(value = "${[null].stream().reduce((x,y)->x).orElse('')}", name = "nullReduce")
@ELExpression(value = "${[null].stream().max().orElse('')}", name = "nullMax")
@ELExpression(value = "${[null].stream().min().orElse('')}", name = "nullMin")
@ELExpression(value = "${[null].stream().findFirst().orElse('')}", name = "nullFindFirst")
@ELExpression(value = "${[3,1,2].stream().max().get()}", name = "streamMax")
@ELExpression(value = "${[3,1,2].stream().min().get()}", name = "streamMin")
@ELExpression(value = "${[3,1,2].stream().findFirst().get()}", name = "streamFindFirst")
// aComparisonThatTheOperandsCannotAnswerIsAnExpressionLanguageError
@ELExpression(value = "${[true, []].stream().min()}", name = "incomparableMin")
@ELExpression(value = "${[[], null].stream().sorted().toList()}", name = "incomparableSorted")
// aSetOrMapConstructionKeepsTheOrderItWasWrittenIn
@ELExpression(value = "${{'b','a'}}", name = "orderedStringSet")
@ELExpression(value = "${{3,1,2}}", name = "orderedNumberSet")
@ELExpression(value = "${{'b':1,'a':2}}", name = "orderedMap")
// aSemicolonExpressionIsNotAnLvalue
@ELExpression(value = "${1 ; bean.name}", name = "semicolonNotLvalue")
// InterpretingELExpressionParserTest
@ELExpression(value = "${0}", name = "zero")
@ELExpression(value = "${extra}", expectedType = Object.class, name = "extra")
public final class ParityInterpreterExpressions {

    private ParityInterpreterExpressions() {
    }
}
