package io.micronaut.el.test;

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import jakarta.el.MethodExpression;
import jakarta.el.MethodInfo;
import jakarta.el.MethodReference;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EqualityExpressionsTest {

    private final Book book = new Book("EL", "reference", 20d);
    private final ELContext context = new CompiledELContext().setBean("book", book);

    @Test
    void compiledExpressionsAreEqualWhenTheyRepresentTheSameExpression() {
        // distinct generated classes, the same expression once whitespace, delimiters and aliases are ignored
        assertEquals(EqualityExpressions$ELExpressions.TITLE, EqualityExpressions$ELExpressions.TITLE_DEFERRED);
        assertEquals(EqualityExpressions$ELExpressions.TITLE, EqualityExpressions$ELExpressions.TITLE_BRACKET);
        assertEquals(EqualityExpressions$ELExpressions.TITLE.hashCode(), EqualityExpressions$ELExpressions.TITLE_BRACKET.hashCode());
        assertEquals(EqualityExpressions$ELExpressions.CHEAP, EqualityExpressions$ELExpressions.CHEAP_SYMBOL);
        // reversed operands are a different expression
        assertNotEquals(EqualityExpressions$ELExpressions.CHEAP, EqualityExpressions$ELExpressions.CHEAP_REVERSED);
        // the same expression declared in another holder is the same expression
        assertEquals(EqualityExpressions$ELExpressions.TITLE, BookExpressions$ELExpressions.TITLE);
        assertNotEquals(EqualityExpressions$ELExpressions.TITLE, EqualityExpressions$ELExpressions.CHEAP);
    }

    @Test
    void aParameteredCompiledMethodExpressionDescribesTheMethodItInvokes() {
        MethodExpression expression = EqualityExpressions$ELExpressions.DISCOUNT_PARAMETERED;
        assertTrue(expression.isParametersProvided());
        assertEquals(18d, expression.invoke(context, null));

        MethodInfo info = expression.getMethodInfo(context);
        assertEquals("discounted", info.getName());
        assertEquals(double.class, info.getReturnType());
        assertArrayEquals(new Class<?>[]{double.class}, info.getParamTypes());

        MethodReference reference = expression.getMethodReference(context);
        assertEquals(book, reference.getBase());
        assertArrayEquals(new Object[]{10L}, reference.getEvaluatedParameters());
    }

    @Test
    void aCompiledMethodExpressionWithDeclaredParameterTypesDescribesTheMethodItInvokes() {
        MethodExpression expression = EqualityExpressions$ELExpressions.DISCOUNT;
        assertEquals(18d, expression.invoke(context, new Object[]{10d}));
        assertEquals("discounted", expression.getMethodInfo(context).getName());
        assertNull(expression.getMethodReference(context).getEvaluatedParameters());
    }
}
