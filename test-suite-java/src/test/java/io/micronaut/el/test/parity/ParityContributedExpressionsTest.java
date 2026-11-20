package io.micronaut.el.test.parity;

import io.micronaut.el.CompiledELContext;
import jakarta.el.MethodExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Asserts the results {@code ELMethodContributorTest} asserts for the interpreted expressions.
 */
class ParityContributedExpressionsTest {

    private final CompiledELContext context = new CompiledELContext()
        .setBean("greeter", new ParityGreeter("ada"))
        .setBean("impl", new ParityDiamond());
    private final CompiledELContext loud = new CompiledELContext()
        .setBean("greeter", new ParityLoudGreeter("ada"));

    @Test
    void theArgumentsAreCoercedToTheDeclaredParameterTypes() {
        assertEquals("hello 42, ada", ParityContributedExpressions$ELExpressions.COERCED_ARGUMENT.getValue(context));
    }

    @Test
    void anOverloadIsSelectedAsTheSpecificationRequires() {
        CompiledELContext overloaded = new CompiledELContext().setBean("greeter", new ParityOverloadedGreeter("ada"));
        assertEquals("hello hello ", ParityOverloadedExpressions$ELExpressions.INTEGER_OVERLOAD.getValue(overloaded));
    }

    @Test
    void aMethodExpressionCarriesTheMetadataOfTheMethod() {
        MethodExpression expression = ParityContributedExpressions$ELExpressions.GREET_METHOD;
        assertEquals("greet", expression.getMethodInfo(context).getName());
        assertEquals(String.class, expression.getMethodInfo(context).getReturnType());
        assertEquals("hello world, ada", expression.invoke(context, new Object[]{"world"}));
    }

    @Test
    void aPropertyResolves() {
        assertEquals("ada", ParityContributedExpressions$ELExpressions.NAME.getValue(context));
    }

    @Test
    void theNearestInterfaceDeclarationWins() {
        assertEquals("nearer", ParityContributedExpressions$ELExpressions.NEAREST_PICK.getValue(context));
    }

    @Test
    void theMethodOfTheSubtypeWinsOverTheSameSignatureOnTheSupertype() {
        assertEquals("child", ParityContributedExpressions$ELExpressions.OVERRIDDEN_PICK.getValue(loud));
    }

    @Test
    void aMethodIsStillInheritedBySubtypes() {
        assertEquals("ada", ParityContributedExpressions$ELExpressions.INHERITED_GET_NAME.getValue(loud));
    }
}
