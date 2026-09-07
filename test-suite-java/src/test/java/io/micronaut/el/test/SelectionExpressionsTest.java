package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The compile-time generated counterparts of the reflective and contributed selection regressions. The
 * compiler resolves these while compiling, so they prove the two execution modes agree rather than that the
 * runtime selection works.
 */
class SelectionExpressionsTest {

    private final ELContext context = new CompiledELContext().setBean("selection", new Selection());

    @Test
    void aNullArgumentSelectsTheReferenceOverloadWhenTheExpressionIsCompiled() {
        // KNOWN DIVERGENCE: the compiler resolves this the way Java does and picks nullable(Object), while
        // the interpreter, Expressly and the Jasper implementation all pick nullable(int) and coerce the null
        // to its default. The differential fuzz test holds the interpreter to the other two; the compiler is
        // the one that disagrees, and changing it changes the code generated for every null argument
        assertEquals("object", SelectionExpressions$ELExpressions.NULL_OVERLOAD.getValue(context));
    }

    @Test
    void theMoreSpecificConstructorIsSelected() {
        assertEquals("number", SelectionExpressions$ELExpressions.CONSTRUCTOR_SPECIFICITY.getValue(context));
    }

    @Test
    void aComparatorLambdaReturningNullYieldsTheDefaultOfItsPrimitiveReturnType() {
        assertEquals("0", SelectionExpressions$ELExpressions.COMPARATOR_NULL.getValue(context));
    }

    @Test
    void aMethodExpressionKeepsThePrimitiveParameterTypesItDeclared() {
        // the neutral annotation metadata drops a primitive class literal; the declaration still selects the
        // int overload, and the arguments are coerced to it
        assertEquals(2, SelectionExpressions$ELExpressions.MATH_MAX.invoke(context, new Object[]{"1", "2"}));
    }

    @Test
    void thePrimitiveParameterTypesSurviveTheExpressionAlias() {
        // the annotation aliases `expression` to `value`, and only the member actually written is recorded
        assertEquals(3, SelectionExpressions$ELExpressions.MATH_MIN.invoke(context, new Object[]{"5", "3"}));
    }

    @Test
    void twoDeclarationsDifferingOnlyByAPrimitiveParameterTypeBothSurvive() {
        // both arrive from the neutral metadata with no parameter types at all, so one would be dropped as a
        // duplicate of the other before the Java mirrors are read
        assertEquals(2, SelectionExpressions$ELExpressions.MATH_MAX.invoke(context, new Object[]{"1", "2"}));
        assertEquals(2L, SelectionExpressions$ELExpressions.MATH_MAX_LONG.invoke(context, new Object[]{"1", "2"}));
    }
}
