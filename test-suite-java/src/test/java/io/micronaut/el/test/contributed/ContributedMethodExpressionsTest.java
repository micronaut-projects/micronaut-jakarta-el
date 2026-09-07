package io.micronaut.el.test.contributed;

import io.micronaut.el.CompiledELContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The compile-time generated counterparts of the interpreted contributor regressions.
 */
class ContributedMethodExpressionsTest {

    private final CompiledELContext context = new CompiledELContext()
        .setBean("greeter", new Greeter("ada"));

    @Test
    void aMethodOfThePlainTypeIsCalled() {
        assertEquals("hello world, ada",
            ContributedMethodExpressions$ELExpressions.GREET.getValue(context));
    }

    @Test
    void aVariableArityMethodPacksItsTrailingArguments() {
        assertEquals("a-b-c", ContributedMethodExpressions$ELExpressions.JOIN.getValue(context));
        assertEquals("", ContributedMethodExpressions$ELExpressions.JOIN_WITHOUT_PARTS.getValue(context));
    }

    @Test
    void aStaticMethodAndAConstructorAreCalled() {
        assertEquals((Object) 7L, ContributedMethodExpressions$ELExpressions.STATIC_ABSOLUTE.getValue(context));
        assertEquals("ada", ContributedMethodExpressions$ELExpressions.CONSTRUCTED.getValue(context));
    }

    @Test
    void aFunctionIsCalled() {
        assertEquals("abab", ContributedMethodExpressions$ELExpressions.TWICE.getValue(context));
    }
}
