package io.micronaut.el.parser;

import io.micronaut.el.parser.ast.BinaryOperator;
import io.micronaut.el.parser.ast.ELNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ELParserTest {

    @Test
    void literalExpression() {
        assertEquals(new ELNode.LiteralText("Aloha!"), ELParser.parse("Aloha!"));
        assertEquals(new ELNode.LiteralText("${exprA}"), ELParser.parse("\\${exprA}"));
    }

    @Test
    void compositeExpression() {
        ELNode node = ELParser.parse("Welcome ${customer.name} to our site");
        ELNode.Composite composite = assertInstanceOf(ELNode.Composite.class, node);
        assertEquals(3, composite.parts().size());
        assertEquals(new ELNode.LiteralText("Welcome "), composite.parts().get(0));
        assertEquals(new ELNode.LiteralText(" to our site"), composite.parts().get(2));
    }

    @Test
    void mixedConstructsAreRejected() {
        assertThrows(ELParsingException.class, () -> ELParser.parse("${a}#{b}"));
    }

    @Test
    void precedence() {
        ELNode node = eval("${1 + 2 * 3}");
        ELNode.Binary add = assertInstanceOf(ELNode.Binary.class, node);
        assertEquals(BinaryOperator.ADD, add.operator());
        assertEquals(BinaryOperator.MULTIPLY, assertInstanceOf(ELNode.Binary.class, add.right()).operator());
    }

    @Test
    void concatenationBindsLooserThanMath() {
        ELNode.Binary concat = assertInstanceOf(ELNode.Binary.class, eval("${1 + 2 += 3}"));
        assertEquals(BinaryOperator.CONCAT, concat.operator());
        assertEquals(BinaryOperator.ADD, assertInstanceOf(ELNode.Binary.class, concat.left()).operator());
    }

    @Test
    void assignmentIsRightAssociative() {
        ELNode.Assign assign = assertInstanceOf(ELNode.Assign.class, eval("${a = b = c}"));
        assertEquals(new ELNode.Identifier("a"), assign.target());
        assertInstanceOf(ELNode.Assign.class, assign.value());
    }

    @Test
    void semicolonOperator() {
        ELNode.Semicolon semicolon = assertInstanceOf(ELNode.Semicolon.class, eval("${a; b}"));
        assertEquals(new ELNode.Identifier("a"), semicolon.left());
    }

    @Test
    void propertyAndMethodAccess() {
        ELNode.Property property = assertInstanceOf(ELNode.Property.class, eval("${a.b}"));
        assertEquals(new ELNode.Identifier("a"), property.base());
        assertEquals(new ELNode.StringLiteral("b"), property.property());

        ELNode.Method method = assertInstanceOf(ELNode.Method.class, eval("${a.b(1, 2)}"));
        assertEquals(2, method.arguments().size());

        ELNode.Property indexed = assertInstanceOf(ELNode.Property.class, eval("${a['b']}"));
        assertEquals(new ELNode.StringLiteral("b"), indexed.property());
    }

    @Test
    void qualifiedFunction() {
        ELNode.Function function = assertInstanceOf(ELNode.Function.class, eval("${fn:length('abc')}"));
        assertEquals("fn", function.prefix());
        assertEquals("length", function.localName());
        assertEquals(1, function.invocations().size());
        assertEquals(1, function.invocations().get(0).size());
    }

    @Test
    void lambdaExpressions() {
        ELNode.Lambda lambda = assertInstanceOf(ELNode.Lambda.class, eval("${x->x+1}"));
        assertEquals(List.of("x"), lambda.parameters());

        ELNode.Lambda nested = assertInstanceOf(ELNode.Lambda.class, eval("${x->y->x+y}"));
        assertInstanceOf(ELNode.Lambda.class, nested.body());

        ELNode.Call call = assertInstanceOf(ELNode.Call.class, eval("${((x,y)->x+y)(3,4)}"));
        assertEquals(2, call.arguments().size());
        assertEquals(List.of("x", "y"), assertInstanceOf(ELNode.Lambda.class, call.target()).parameters());

        ELNode.Lambda noArgs = assertInstanceOf(ELNode.Lambda.class, eval("${()->64}"));
        assertTrue(noArgs.parameters().isEmpty());
    }

    @Test
    void collectionConstruction() {
        assertEquals(3, assertInstanceOf(ELNode.SetData.class, eval("${{1, 2, 3}}")).elements().size());
        assertEquals(3, assertInstanceOf(ELNode.ListData.class, eval("${[1, 'two', 3]}")).elements().size());
        assertEquals(2, assertInstanceOf(ELNode.MapData.class, eval("${{'one':1, 'two':2}}")).entries().size());
        assertTrue(assertInstanceOf(ELNode.SetData.class, eval("${{}}")).elements().isEmpty());
    }

    @Test
    void streamPipeline() {
        ELNode node = eval("${books.stream().filter(b->b.category == 'history').map(b->b.title).toList()}");
        assertInstanceOf(ELNode.Method.class, node);
    }

    @Test
    void conditionalWithQualifiedFunctionIsIllegal() {
        assertThrows(ELParsingException.class, () -> ELParser.parse("${c?b:f()}"));
        assertInstanceOf(ELNode.Ternary.class, eval("${c?b:(f())}"));
    }

    @Test
    void unaryOperators() {
        assertInstanceOf(ELNode.Unary.class, eval("${empty a}"));
        assertInstanceOf(ELNode.Unary.class, eval("${not a}"));
        assertInstanceOf(ELNode.Unary.class, eval("${-a}"));
    }

    @Test
    void numericLiterals() {
        assertEquals(new ELNode.IntegerLiteral("42"), eval("${42}"));
        assertEquals(new ELNode.FloatingPointLiteral("4.2"), eval("${4.2}"));
        assertEquals(new ELNode.FloatingPointLiteral("1e5"), eval("${1e5}"));
        assertEquals(new ELNode.FloatingPointLiteral(".5"), eval("${.5}"));
    }

    @Test
    void stringLiteralEscapes() {
        assertEquals(new ELNode.StringLiteral("it's"), eval("${'it\\'s'}"));
        assertEquals(new ELNode.StringLiteral("a\"b"), eval("${\"a\\\"b\"}"));
    }

    @Test
    void nestedMapInExpression() {
        ELNode node = eval("${{'a':{'b':1}}}");
        ELNode.MapData map = assertInstanceOf(ELNode.MapData.class, node);
        assertInstanceOf(ELNode.MapData.class, map.entries().get(0).value());
    }

    private static ELNode eval(String expression) {
        return assertInstanceOf(ELNode.Eval.class, ELParser.parse(expression)).expression();
    }
}
