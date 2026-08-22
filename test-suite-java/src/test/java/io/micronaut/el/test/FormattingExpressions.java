package io.micronaut.el.test;

import io.micronaut.el.annotation.ELExpression;

/**
 * The expressions reach the formatter through the resolver chain, as a message interpolator would.
 */
@ELExpression(value = "${f.format('%.2f', 0.5)}", expectedType = String.class, name = "FORMATTED")
@ELExpression(value = "${f.join('-', 'a', 'b', 'c')}", expectedType = String.class, name = "JOINED")
@ELExpression(value = "${f.join('-')}", expectedType = String.class, name = "JOINED_ALONE")
public final class FormattingExpressions {
}
