package io.micronaut.el.test;

import io.micronaut.el.annotation.ELExpression;

@ELExpression(value = "${1 + 2}", name = "add")
@ELExpression(value = "${1 + 2 * 3}", name = "precedence")
@ELExpression(value = "${7 / 2}", name = "divide")
@ELExpression(value = "${7 mod 2}", name = "modulo")
@ELExpression(value = "${-3}", name = "negate")
@ELExpression(value = "${'a' += 'b' += 'c'}", name = "concat")
@ELExpression(value = "${1 lt 2}", name = "lessThan")
@ELExpression(value = "${'10' == 10}", name = "coercedEquality")
@ELExpression(value = "${null == null}", name = "nullEquality")
@ELExpression(value = "${true and false}", name = "and")
@ELExpression(value = "${true or false}", name = "or")
@ELExpression(value = "${not true}", name = "not")
@ELExpression(value = "${empty ''}", name = "emptyString")
@ELExpression(value = "${empty [1]}", name = "emptyList")
@ELExpression(value = "${true ? 'yes' : 'no'}", name = "ternary")
@ELExpression(value = "${1; 2; 3}", name = "semicolon")
@ELExpression(value = "${[1, 'two', 3]}", name = "list")
@ELExpression(value = "${{1, 2, 2, 3}}", name = "set")
@ELExpression(value = "${{'one':1, 'two':2}}", name = "map")
@ELExpression(value = "${(1 + 2) * 3}", name = "parentheses")
@ELExpression(value = "${1.5 + 1}", name = "floatingPoint")
public final class OperatorExpressions {

    private OperatorExpressions() {
    }
}
