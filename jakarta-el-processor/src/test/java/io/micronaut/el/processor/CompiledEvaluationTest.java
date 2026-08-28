package io.micronaut.el.processor;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.ELExpressionSource;
import jakarta.el.ELContext;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * What the compiled expressions evaluate to, compiled by the processor in this build rather than in the one of
 * a test suite, so that the paths the compiler takes for each kind of expression are exercised here.
 */
class CompiledEvaluationTest {

    private static final String CLASS_NAME = "io.micronaut.el.test.evaluated.Expressions";

    /**
     * The declaring class: a variable of every type a class literal can name, so that the types of the
     * declarations resolve, and the members the expressions below reach.
     */
    private static final String SOURCE = """
        package io.micronaut.el.test.evaluated;

        import io.micronaut.el.annotation.*;

        @ELEnvironment(
            variables = {
                @ELVariable(name = "flag", type = boolean.class),
                @ELVariable(name = "b", type = byte.class),
                @ELVariable(name = "s", type = short.class),
                @ELVariable(name = "i", type = int.class),
                @ELVariable(name = "l", type = long.class),
                @ELVariable(name = "c", type = char.class),
                @ELVariable(name = "f", type = float.class),
                @ELVariable(name = "d", type = double.class),
                @ELVariable(name = "flags", type = boolean[].class),
                @ELVariable(name = "bytes", type = byte[].class),
                @ELVariable(name = "shorts", type = short[].class),
                @ELVariable(name = "ints", type = int[].class),
                @ELVariable(name = "longs", type = long[].class),
                @ELVariable(name = "chars", type = char[].class),
                @ELVariable(name = "floats", type = float[].class),
                @ELVariable(name = "doubles", type = double[].class),
                @ELVariable(name = "words", type = String[].class),
                @ELVariable(name = "grid", type = int[][].class),
                @ELVariable(name = "text", type = String.class),
                @ELVariable(name = "big", type = java.math.BigInteger.class),
                @ELVariable(name = "decimal", type = java.math.BigDecimal.class),
                @ELVariable(name = "titles", type = java.util.List.class),
                @ELVariable(name = "ages", type = java.util.Map.class),
                @ELVariable(name = "bean", type = Expressions.Bean.class)
            },
            imports = Point.class,
            staticImports = Text.class,
            functions = @ELFunctions(value = Text.class, prefix = "t")
        )

        // literals, operators and coercion
        @ELExpression(value = "${ 1 + 2 }", expectedType = Long.class, name = "addition")
        @ELExpression(value = "${ 7 / 2 }", expectedType = Double.class, name = "division")
        @ELExpression(value = "${ 7 mod 2 }", expectedType = Long.class, name = "modulo")
        @ELExpression(value = "${ -i }", expectedType = Integer.class, name = "negation")
        @ELExpression(value = "${ big + 1 }", expectedType = java.math.BigInteger.class, name = "bigAddition")
        @ELExpression(value = "${ decimal * 2 }", expectedType = java.math.BigDecimal.class, name = "decimalProduct")
        @ELExpression(value = "${ text += '!' }", expectedType = String.class, name = "concatenation")
        @ELExpression(value = "${ i lt l }", expectedType = Boolean.class, name = "lessThan")
        @ELExpression(value = "${ text eq 'EL' }", expectedType = Boolean.class, name = "equality")
        @ELExpression(value = "${ flag and not flag }", expectedType = Boolean.class, name = "logical")
        @ELExpression(value = "${ empty bean.absent }", expectedType = Boolean.class, name = "emptyBean")
        @ELExpression(value = "${ empty titles }", expectedType = Boolean.class, name = "emptyList")
        @ELExpression(value = "${ flag ? 'yes' : 'no' }", expectedType = String.class, name = "conditional")
        @ELExpression(value = "${ i > 1 ? i : l }", expectedType = Long.class, name = "mixedConditional")

        // the primitive variables, which the compiler reads without boxing
        @ELExpression(value = "${ b + s }", expectedType = Integer.class, name = "byteAndShort")
        @ELExpression(value = "${ c }", expectedType = String.class, name = "character")
        @ELExpression(value = "${ f + d }", expectedType = Double.class, name = "floatAndDouble")

        // arrays, whose access the specification defines reflectively
        @ELExpression(value = "${ flags[0] }", expectedType = Boolean.class, name = "firstFlag")
        @ELExpression(value = "${ bytes[0] + shorts[0] }", expectedType = Integer.class, name = "firstBytes")
        @ELExpression(value = "${ ints[1] }", expectedType = Integer.class, name = "secondInt")
        @ELExpression(value = "${ longs[0] + chars[0] }", expectedType = Long.class, name = "firstLongs")
        @ELExpression(value = "${ floats[0] + doubles[0] }", expectedType = Double.class, name = "firstFloats")
        @ELExpression(value = "${ words[0] }", expectedType = String.class, name = "firstWord")
        @ELExpression(value = "${ grid[1][0] }", expectedType = Integer.class, name = "gridCell")

        // properties and the null base that short-circuits them
        @ELExpression(value = "${ bean.title }", expectedType = String.class, name = "title")
        @ELExpression(value = "${ bean.nested.title }", expectedType = String.class, name = "nestedTitle")
        @ELExpression(value = "${ bean.absent.title }", expectedType = String.class, name = "absentTitle")
        @ELExpression(value = "${ bean.absent.count }", expectedType = Integer.class, name = "absentCount")
        @ELExpression(value = "${ bean['title'] }", expectedType = String.class, name = "titleByKey")
        @ELExpression(value = "${ titles[0] }", expectedType = String.class, name = "firstTitle")
        @ELExpression(value = "${ ages['ada'] }", expectedType = Integer.class, name = "age")

        // methods, including the overloads the selection has to choose between
        @ELExpression(value = "${ bean.describe() }", expectedType = String.class, name = "describe")
        @ELExpression(value = "${ bean.pick(1) }", expectedType = String.class, name = "pickLong")
        @ELExpression(value = "${ bean.pick('one') }", expectedType = String.class, name = "pickString")
        @ELExpression(value = "${ bean.widen(1) }", expectedType = String.class, name = "widen")
        @ELExpression(value = "${ bean.join('-', 'a', 'b') }", expectedType = String.class, name = "joinExpanded")
        @ELExpression(value = "${ bean.join('-') }", expectedType = String.class, name = "joinAlone")
        @ELExpression(value = "${ bean.join('-', words) }", expectedType = String.class, name = "joinArray")
        @ELExpression(value = "${ bean.reset() }", expectedType = String.class, name = "voidCall")
        @ELExpression(value = "${ bean.absent.describe() }", expectedType = String.class, name = "absentDescribe")

        // collections, lambdas and streams
        @ELExpression(value = "${ [1, 2, 3] }", expectedType = java.util.List.class, name = "listData")
        @ELExpression(value = "${ {1, 2, 2} }", expectedType = java.util.Set.class, name = "setData")
        @ELExpression(value = "${ {'a': 1, 'b': 2} }", expectedType = java.util.Map.class, name = "mapData")
        @ELExpression(value = "${ ((x, y) -> x + y)(1, 2) }", expectedType = Long.class, name = "lambdaCall")
        @ELExpression(value = "${ titles.stream().filter(x -> x.length() > 2).toList() }",
            expectedType = java.util.List.class, name = "streamFilter")
        @ELExpression(value = "${ [3, 1, 2].stream().sorted().toList() }",
            expectedType = java.util.List.class, name = "streamSorted")
        @ELExpression(value = "${ [1, 2, 3].stream().map(x -> x * 2).sum() }",
            expectedType = Long.class, name = "streamSum")
        @ELExpression(value = "${ [1, 2].stream().findFirst().get() }", expectedType = Long.class,
            name = "streamFirst")

        // imports, constructors and functions
        @ELExpression(value = "${ Point(1, 2).sum }", expectedType = Integer.class, name = "constructed")
        @ELExpression(value = "${ shout(text) }", expectedType = String.class, name = "staticImport")
        @ELExpression(value = "${ t:shout('el') }", expectedType = String.class, name = "prefixedFunction")

        // assignment and the semicolon operator, which have side effects
        @ELExpression(value = "${ bean.title = 'assigned'; bean.title }", expectedType = String.class,
            name = "assignment")
        public class Expressions {

            public static class Bean {

                private String title = "EL";

                public String getTitle() { return title; }
                public void setTitle(String title) { this.title = title; }
                public int getCount() { return 2; }
                public Bean getNested() { return this; }
                public Bean getAbsent() { return null; }
                public String describe() { return "described"; }
                public String pick(Long value) { return "long"; }
                public String pick(String value) { return "string"; }
                public String widen(Number value) { return "number"; }
                public String widen(Object value) { return "object"; }
                public String join(String separator, Object... parts) {
                    return separator + parts.length;
                }
                public String join(String separator) { return "only" + separator; }
                public void reset() { title = "EL"; }
            }

        }

        class Point {

            private final int x;
            private final int y;

            public Point(int x, int y) { this.x = x; this.y = y; }

            public int getSum() { return x + y; }
        }

        class Text {
            public static String shout(String value) { return value.toUpperCase(); }
        }
        """;

    private static GeneratedExpressions generated;
    private static ELContext context;

    @BeforeAll
    static void compile() throws Exception {
        generated = GeneratedExpressions.of(CLASS_NAME, SOURCE);
        context = new CompiledELContext()
            .setBean("flag", true)
            .setBean("b", (byte) 1)
            .setBean("s", (short) 2)
            .setBean("i", 3)
            .setBean("l", 4L)
            .setBean("c", 'x')
            .setBean("f", 1.5f)
            .setBean("d", 2.5d)
            .setBean("flags", new boolean[]{true, false})
            .setBean("bytes", new byte[]{1, 2})
            .setBean("shorts", new short[]{3, 4})
            .setBean("ints", new int[]{5, 6})
            .setBean("longs", new long[]{7L, 8L})
            .setBean("chars", new char[]{'a', 'b'})
            .setBean("floats", new float[]{1.5f, 2.5f})
            .setBean("doubles", new double[]{0.5d, 1.5d})
            .setBean("words", new String[]{"first", "second"})
            .setBean("grid", new int[][]{{1, 2}, {3, 4}})
            .setBean("text", "EL")
            .setBean("big", new BigInteger("9007199254740993"))
            .setBean("decimal", new BigDecimal("1.5"))
            .setBean("titles", List.of("EL", "spec"))
            .setBean("ages", Map.of("ada", 36))
            .setBean("bean", generated.instantiate(CLASS_NAME + "$Bean"));
    }

    private static Object value(String expression, Class<?> expectedType) {
        ELExpressionSource registry = generated.registry();
        ValueExpression compiled = registry.createValueExpression(context, expression, expectedType);
        assertNotNull(compiled, "the registry does not declare " + expression);
        return compiled.getValue(context);
    }

    @Test
    void literalsAndOperators() {
        assertEquals(3L, value("${ 1 + 2 }", Long.class));
        assertEquals(3.5d, value("${ 7 / 2 }", Double.class));
        assertEquals(1L, value("${ 7 mod 2 }", Long.class));
        assertEquals(-3, value("${ -i }", Integer.class));
        assertEquals(new BigInteger("9007199254740994"), value("${ big + 1 }", BigInteger.class));
        assertEquals(new BigDecimal("3.0"), value("${ decimal * 2 }", BigDecimal.class));
        assertEquals("EL!", value("${ text += '!' }", String.class));
        assertEquals(true, value("${ i lt l }", Boolean.class));
        assertEquals(true, value("${ text eq 'EL' }", Boolean.class));
        assertEquals(false, value("${ flag and not flag }", Boolean.class));
        assertEquals(true, value("${ empty bean.absent }", Boolean.class));
        assertEquals(false, value("${ empty titles }", Boolean.class));
        assertEquals("yes", value("${ flag ? 'yes' : 'no' }", String.class));
        assertEquals(3L, value("${ i > 1 ? i : l }", Long.class));
    }

    @Test
    void theDeclaredPrimitivesKeepTheirTypes() {
        assertEquals(3, value("${ b + s }", Integer.class));
        assertEquals("x", value("${ c }", String.class));
        assertEquals(4d, value("${ f + d }", Double.class));
    }

    @Test
    void arrayAccess() {
        assertEquals(true, value("${ flags[0] }", Boolean.class));
        assertEquals(4, value("${ bytes[0] + shorts[0] }", Integer.class));
        assertEquals(6, value("${ ints[1] }", Integer.class));
        assertEquals(104L, value("${ longs[0] + chars[0] }", Long.class));
        assertEquals(2d, value("${ floats[0] + doubles[0] }", Double.class));
        assertEquals("first", value("${ words[0] }", String.class));
        assertEquals(3, value("${ grid[1][0] }", Integer.class));
    }

    @Test
    void propertiesAndTheNullBasesThatShortCircuitThem() {
        assertEquals("EL", value("${ bean.title }", String.class));
        assertEquals("EL", value("${ bean.nested.title }", String.class));
        // a null base short-circuits the access, and the null result is coerced to the expected type
        assertEquals("", value("${ bean.absent.title }", String.class));
        assertNull(value("${ bean.absent.count }", Integer.class));
        assertEquals("EL", value("${ bean['title'] }", String.class));
        assertEquals("EL", value("${ titles[0] }", String.class));
        assertEquals(36, value("${ ages['ada'] }", Integer.class));
    }

    @Test
    void methodsAndTheOverloadsTheSelectionChoosesBetween() {
        assertEquals("described", value("${ bean.describe() }", String.class));
        assertEquals("long", value("${ bean.pick(1) }", String.class));
        assertEquals("string", value("${ bean.pick('one') }", String.class));
        assertEquals("number", value("${ bean.widen(1) }", String.class));
        assertEquals("-2", value("${ bean.join('-', 'a', 'b') }", String.class));
        assertEquals("only-", value("${ bean.join('-') }", String.class));
        // an array of a component type the parameter does not declare is packed, not passed through
        assertEquals("-1", value("${ bean.join('-', words) }", String.class));
        assertEquals("", value("${ bean.reset() }", String.class));
        assertEquals("", value("${ bean.absent.describe() }", String.class));
    }

    @Test
    void collectionsLambdasAndStreams() {
        assertEquals(List.of(1L, 2L, 3L), value("${ [1, 2, 3] }", List.class));
        assertEquals(Set.of(1L, 2L), value("${ {1, 2, 2} }", Set.class));
        assertEquals(Map.of("a", 1L, "b", 2L), value("${ {'a': 1, 'b': 2} }", Map.class));
        assertEquals(3L, value("${ ((x, y) -> x + y)(1, 2) }", Long.class));
        assertEquals(List.of("spec"), value("${ titles.stream().filter(x -> x.length() > 2).toList() }", List.class));
        assertEquals(List.of(1L, 2L, 3L), value("${ [3, 1, 2].stream().sorted().toList() }", List.class));
        assertEquals(12L, value("${ [1, 2, 3].stream().map(x -> x * 2).sum() }", Long.class));
        assertEquals(1L, value("${ [1, 2].stream().findFirst().get() }", Long.class));
    }

    @Test
    void importsConstructorsAndFunctions() {
        assertEquals(3, value("${ Point(1, 2).sum }", Integer.class));
        assertEquals("EL", value("${ shout(text) }", String.class));
        assertEquals("EL", value("${ t:shout('el') }", String.class));
    }

    @Test
    void theAssignmentAndTheSemicolonRunInOrder() {
        assertEquals("assigned", value("${ bean.title = 'assigned'; bean.title }", String.class));
        // the bean is shared with the other cases, so it is put back the way they expect it
        assertEquals("", value("${ bean.reset() }", String.class));
    }
}
