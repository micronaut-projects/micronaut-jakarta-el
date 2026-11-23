/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.el.interpreter;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.ELSandbox;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELProcessor;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.PropertyNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The interpreted counterparts of the compiler regressions: the expressions of
 * {@code io.micronaut.el.test.CompilerRegressionExpressions}, and of the Groovy, Kotlin and custom annotation
 * regressions, with the same text and the same results, evaluated by the interpreter. The two execution modes
 * are two translations of one specification, and these pin them to the same answer.
 */
class CompilerRegressionInterpretedTest {

    private final ExpressionFactory factory = new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser());
    private final ELProcessor processor = new ELProcessor();

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        CompilerRegressionProbe.LOG.clear();
        processor.defineBean("probe", new CompilerRegressionProbe());
        processor.getELManager().importClass(CompilerRegressionResource.class.getName());
        processor.getELManager().importStatic(CompilerRegressionFunctions.class.getName() + ".cmp");
        for (var method : List.of(
            CompilerRegressionFunctions.class.getMethod("hitS", String.class),
            CompilerRegressionFunctions.class.getMethod("nothing"),
            CompilerRegressionFunctions.class.getMethod("vs", String[].class),
            CompilerRegressionFunctions.class.getMethod("cmp", Comparable.class),
            CompilerRegressionFunctions.class.getMethod("ser", Serializable.class),
            CompilerRegressionFunctions.class.getMethod("list", List.class),
            CompilerRegressionFunctions.class.getMethod("read", String.class))) {
            processor.defineFunction("fn", method.getName(), method);
        }
        // the sandbox is a policy of the interpreted path, not a semantic difference, and this compares semantics
        processor.getELManager().getELContext().putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);
    }

    private Object value(String expression) {
        ELContext context = processor.getELManager().getELContext();
        return factory.createValueExpression(context, expression, Object.class).getValue(context);
    }

    private static List<String> log() {
        return CompilerRegressionProbe.LOG;
    }

    @Test
    void theOperandsThatAreNotTakenAreNotEvaluated() {
        assertEquals("t", value("${probe.flag ? probe.hit('t').trim() : probe.hit('f').trim()}"));
        assertEquals(List.of("t"), log());
        log().clear();
        assertEquals(false, value("${false && probe.hit('a').length() > 0}"));
        assertEquals(true, value("${true || probe.yes('c')}"));
        assertEquals(List.of(), log());
        assertEquals("n", value("${empty probe.tags ? probe.hit('e') : probe.hit('n')}"));
        assertEquals(List.of("n"), log());
        log().clear();
        assertEquals("EL", value("${probe.title += (probe.flag ? '' : probe.hit('never'))}"));
        assertEquals(List.of(), log());
    }

    @Test
    void anArgumentIsEvaluatedOnce() {
        assertEquals("str:arg", value("${probe.takeString(probe.hit('arg'))}"));
        assertEquals(List.of("arg"), log());
    }

    @Test
    void theOperandsAreEvaluatedFromLeftToRight() {
        assertEquals("xy", value("${fn:hitS('x') += probe.hit('y')}"));
        assertEquals(List.of("x", "y"), log());
        log().clear();
        assertEquals(List.of("x", "y"), value("${[fn:hitS('x'), probe.hit('y')]}"));
        assertEquals(List.of("x", "y"), log());
        log().clear();
        assertEquals(false, value("${fn:hitS('x') == probe.hit('y')}"));
        assertEquals(List.of("x", "y"), log());
    }

    @Test
    void aPrimitivePropertyOfANullBaseIsNull() {
        assertEquals(false, value("${probe.nullBean.pages == 0}"));
        assertEquals(false, value("${probe.nullBean.pages < 1}"));
        assertEquals(false, value("${probe.nullBean.pages <= 0}"));
        assertEquals(false, value("${probe.nullBean.flag == false}"));
        assertEquals(0L, value("${probe.probes.stream().filter(p -> p.nullBean.pages == 0).count()}"));
        assertEquals(0L, value("${-probe.nullBean.pages}"));
        assertEquals(1L, value("${probe.nullBean.pages + 1}"));
    }

    @Test
    void aSingleArgumentOfAVariableArityMethod() {
        assertEquals("EL#1", value("${probe.join(probe.title)}"));
        assertEquals("p+q#2", value("${probe.join(probe.codes)}"));
        assertEquals("a#1", value("${fn:vs('a')}"));
    }

    @Test
    void anIdentifierThatIsNotEvaluatedIsNotResolved() {
        assertEquals(true, value("${true || (undefinedVar.a == undefinedVar.b)}"));
    }

    @Test
    void aLambdaParameterHoldingADollar() {
        assertEquals(List.of("a", "b", "c"), value("${probe.tags.stream().map($x -> $x).toList()}"));
    }

    @Test
    void aStaticMethodInvokedThroughAnInstance() {
        assertEquals("static", value("${probe.stat()}"));
        assertEquals("5", value("${probe.title.valueOf(5)}"));
    }

    @Test
    void aVoidFunctionEvaluatesToNull() {
        assertNull(value("${fn:nothing()}"));
        assertEquals(List.of("nothing"), log());
    }

    @Test
    void argumentsTheRuntimeCoercionAccepts() {
        assertEquals("cmp:300", value("${fn:cmp(probe.pages)}"));
        assertEquals("cmp:5", value("${fn:cmp(probe.number)}"));
        assertEquals("ser:true", value("${fn:ser(probe.flag)}"));
        assertEquals("cmp:cs", value("${fn:cmp(probe.charSequence)}"));
        assertThrows(ELException.class, () -> value("${fn:list(probe.charSequence)}"));
        assertEquals("cmp:5", value("${cmp(probe.number)}"));
        assertEquals("resource:5", value("${CompilerRegressionResource(probe.number).name}"));
    }

    @Test
    void aLambdaReturningNullToATypeVariablePassesNull() {
        assertEquals("got:null", value("${probe.supply(() -> null)}"));
        assertEquals("got:null", value("${probe.fn(x -> null)}"));
    }

    @Test
    void whatAMemberThrowsIsAnELException() {
        ELException thrown = assertThrows(ELException.class, () -> value("${probe.boom()}"));
        assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertEquals("ok", value("${probe.checked('ok')}"));
        assertInstanceOf(IOException.class,
            assertThrows(ELException.class, () -> value("${probe.checked('fail')}")).getCause());
        assertInstanceOf(IOException.class,
            assertThrows(ELException.class, () -> value("${fn:read('fail')}")).getCause());
        assertInstanceOf(IOException.class,
            assertThrows(ELException.class, () -> value("${CompilerRegressionResource('fail')}")).getCause());
    }

    @Test
    void thePropertyOfAnOptionalIsTheOneOfItsValue() {
        // the resolvers of the context the compiled expressions run with, where the OptionalELResolver precedes
        // the BeanELResolver; the standard context of an ELProcessor reads isPresent() as a bean property
        CompiledELContext context = new CompiledELContext().setBean("probe", new CompilerRegressionProbe());
        context.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);
        assertThrows(PropertyNotFoundException.class, () -> factory
            .createValueExpression(context, "${probe.optionalTitle.present}", Object.class).getValue(context));
    }

    @Test
    void theMethodOfAnOptionalIsTheOneOfItsValue() {
        assertEquals(3, value("${probe.optionalTitle.length()}"));
        assertNull(value("${probe.emptyTitle.length()}"));
    }

    @Test
    void aNullArgumentSelectsThePrimitiveOverload() {
        assertEquals("int", value("${probe.nullable(null)}"));
        // the expression of io.micronaut.el.test.SelectionExpressions, against a bean of the same shape
        processor.defineBean("selection", new CompilerRegressionProbe());
        assertEquals("int", value("${selection.nullable(null)}"));
    }

    /**
     * The counterparts of the Groovy and Kotlin regressions, whose declarations name primitive class literals:
     * a variable of a primitive type, and method expressions selecting the overload by primitive parameters.
     */
    @Test
    void primitiveVariablesAndParameterTypes() {
        ELContext context = processor.getELManager().getELContext();
        processor.defineBean("count", 2);
        processor.defineBean("text", new Text());
        assertEquals(3L, value("${count + 1}"));
        // the value of the variable is an Integer, whatever primitive type the Kotlin declaration names
        assertEquals("Integer:2", value("${text.which(count)}"));
        MethodExpression single = factory.createMethodExpression(context, "${text.repeat}", String.class,
            new Class<?>[]{int.class});
        assertEquals("int:abab", single.invoke(context, new Object[]{2}));
        MethodExpression mixed = factory.createMethodExpression(context, "${text.pad}", String.class,
            new Class<?>[]{int.class, String.class});
        assertEquals("int:--", mixed.invoke(context, new Object[]{2, "-"}));
        // the Kotlin Int::class is int.class: the primitive overload, not the wrapper one
        MethodExpression which = factory.createMethodExpression(context, "${text.which}", String.class,
            new Class<?>[]{int.class});
        assertEquals("int:2", which.invoke(context, new Object[]{2}));
        MethodExpression whichLong = factory.createMethodExpression(context, "${text.whichLong}", String.class,
            new Class<?>[]{long.class, boolean.class, double.class});
        assertEquals("primitives:1:true:0.5", whichLong.invoke(context, new Object[]{1L, true, 0.5}));
    }

    /**
     * The counterparts of the custom annotation regressions: the expressions as the remappers store them, every
     * {@code ${...}} of a composite stored as {@code #{...}}, evaluate to what the generated ones do.
     */
    @Test
    void theExpressionsAsTheRemappersStoreThem() {
        processor.defineBean("customer", Map.of("name", "Ann"));
        processor.defineBean("amount", 0L);
        assertEquals(false, value("#{ amount > 0 }"));
        assertEquals("Sorry Ann, 0 is not enough", value("Sorry #{customer.name}, #{amount} is not enough"));
        processor.defineBean("validatedValue", 3L);
        assertEquals("3", factory.createValueExpression(processor.getELManager().getELContext(),
            "#{validatedValue}", String.class).getValue(processor.getELManager().getELContext()));
    }

    /**
     * The compiler rejects these when it compiles them, and now reports each on the member declaring it, all of
     * them at once (CompilerRegressionProcessorTest), so no generated expression can exist for them. The closest
     * runtime contract is the interpreter failing on evaluation the same expressions.
     */
    @Test
    void whatTheCompilerRejectsFailsWhenInterpreted() {
        assertThrows(ELException.class, () -> value("${fn:missing(1)}"));
        assertThrows(PropertyNotFoundException.class, () -> value("${probe.nope}"));
    }

    /**
     * The bean of the primitive parameter regressions.
     */
    public static final class Text {

        public String repeat(int times) {
            return "int:" + "ab".repeat(times);
        }

        public String repeat(String times) {
            return "string:" + times;
        }

        public String pad(int width, String fill) {
            return "int:" + fill.repeat(width);
        }

        public String pad(long width, String fill) {
            return "long:" + fill.repeat((int) width);
        }

        public String which(int value) {
            return "int:" + value;
        }

        public String which(Integer value) {
            return "Integer:" + value;
        }

        public String whichLong(long value, boolean flag, double ratio) {
            return "primitives:" + value + ":" + flag + ":" + ratio;
        }

        public String whichLong(Long value, Boolean flag, Double ratio) {
            return "wrappers:" + value + ":" + flag + ":" + ratio;
        }
    }
}
