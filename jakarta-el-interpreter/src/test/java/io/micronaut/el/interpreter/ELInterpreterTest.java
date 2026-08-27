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

import io.micronaut.el.CompiledExpressionFactory;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELProcessor;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodExpression;
import jakarta.el.StandardELContext;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ELInterpreterTest {

    private final ELProcessor processor = new ELProcessor();
    private final ExpressionFactory factory = new CompiledExpressionFactory(List.of(),
        new InterpretingELExpressionParser());

    @Test
    void literalsAndArithmetic() {
        assertEquals((Object) 3L, processor.eval("1 + 2"));
        assertEquals((Object) 2.5d, processor.eval("5 / 2"));
        assertEquals((Object) 1L, processor.eval("7 mod 3"));
        assertEquals((Object) Long.valueOf(-3L), processor.eval("-(1 + 2)"));
        assertEquals("ab", processor.eval("'a' += 'b'"));
    }

    @Test
    void relationalAndLogicalOperators() {
        assertEquals((Object) true, processor.eval("1 lt 2"));
        assertEquals((Object) true, processor.eval("'a' == 'a'"));
        assertEquals((Object) false, processor.eval("null == 1"));
        assertEquals((Object) true, processor.eval("true and not false"));
        assertEquals((Object) true, processor.eval("empty null"));
        assertEquals((Object) true, processor.eval("empty []"));
    }

    @Test
    void aBooleanOperandDecidesARelationalComparison() {
        // the section 1.9.1 orders its rules the way the equality of the section 1.9.2 does, so a boolean
        // operand is coerced with the other rather than the two being compared as strings
        assertEquals((Object) false, processor.eval("false gt '9'"));
        assertEquals((Object) true, processor.eval("false le '9'"));
        assertEquals((Object) false, processor.eval("false lt '9'"));
        assertEquals((Object) true, processor.eval("false ge '9'"));
        assertEquals((Object) false, processor.eval("'9' gt false"));
        assertEquals((Object) true, processor.eval("true gt '9'"));
        assertEquals((Object) true, processor.eval("true gt false"));
        assertEquals((Object) false, processor.eval("true lt false"));
        assertEquals((Object) true, processor.eval("true ge true"));
    }

    @Test
    void aCharacterIsANumberForTheUnaryMinus() {
        // the section 1.25.3 coerces a character to its numeric value, which the binary operators already do
        assertEquals((Object) (short) -49, processor.eval("-Character.valueOf(49)"));
        assertEquals((Object) 49L, processor.eval("Character.valueOf(49) + 0"));
        assertEquals((Object) (-49L), processor.eval("-Character.valueOf(49) + 0"));
    }

    @Test
    void conditionalAndSemicolonOperators() {
        assertEquals("yes", processor.eval("true ? 'yes' : 'no'"));
        assertEquals((Object) 2L, processor.eval("1; 2"));
    }

    @Test
    void anOverloadIsSelectedTheWayTheSpecificationSelectsIt() {
        // the coercions of the section 1.25 make several overloads applicable, and the section 1.6 reduces
        // them to the most specific one rather than calling the reference ambiguous
        assertEquals((Object) 1, processor.eval("Integer.valueOf(1)"));
        assertEquals((Object) 1L, processor.eval("Long.valueOf(1)"));
        assertEquals((Object) 1.0d, processor.eval("Double.valueOf(1)"));
        assertEquals("1", processor.eval("String.valueOf(1)"));
        assertEquals((Object) 'A', processor.eval("Character.valueOf(65)"));
        // a number is not coercible to a boolean, so valueOf(boolean) is not a candidate at all and
        // valueOf(String) is the only one left
        assertEquals((Object) false, processor.eval("Boolean.valueOf(1)"));
        assertEquals((Object) true, processor.eval("Boolean.valueOf('true')"));
    }

    @Test
    void aStreamHoldingANullElementDoesNotFail() {
        // Stream.reduce, Stream.max, Stream.min and Stream.findFirst of the platform wrap their result in an
        // Optional, which cannot hold a null, where the optional of the section 2.3 is empty for one
        assertEquals("", processor.eval("[null].stream().reduce((x,y)->x).orElse('')"));
        assertEquals("", processor.eval("[null].stream().max().orElse('')"));
        assertEquals("", processor.eval("[null].stream().min().orElse('')"));
        assertEquals("", processor.eval("[null].stream().findFirst().orElse('')"));
        assertEquals((Object) 3L, processor.eval("[3,1,2].stream().max().get()"));
        assertEquals((Object) 1L, processor.eval("[3,1,2].stream().min().get()"));
        assertEquals((Object) 3L, processor.eval("[3,1,2].stream().findFirst().get()"));
    }

    @Test
    void aComparisonThatTheOperandsCannotAnswerIsAnExpressionLanguageError() {
        // the section 1.9.1 of the specification: when compareTo fails, the failure is an error of the
        // language, not the ClassCastException the comparison happened to raise
        assertThrows(ELException.class, () -> processor.eval("[true, []].stream().min()"));
        assertThrows(ELException.class, () -> processor.eval("[[], null].stream().sorted().toList()"));
    }

    @Test
    void theOperandsOfARelationalOperatorAreBothEvaluated() {
        // only the section 1.10 and the section 1.11 specify a short circuit, for the logical operators and
        // for the conditional. A relational operator whose left operand is null answers false whatever the
        // right one is, but skipping it would drop whatever it does on the way
        assertThrows(ELException.class, () -> processor.eval("null gt undefinedIdentifier"));
        assertThrows(ELException.class, () -> processor.eval("null lt undefinedIdentifier"));
        assertEquals((Object) false, processor.eval("y = 1; null gt (y = 2)"));
        assertEquals((Object) 2L, processor.eval("y = 1; null gt (y = 2); y"));
    }

    @Test
    void aSetOrMapConstructionKeepsTheOrderItWasWrittenIn() {
        // the section 2.2 leaves the iteration order of a construction open, and an order that is the order
        // of the expression is the one that does not surprise
        assertEquals("[b, a]", processor.eval("{'b','a'}").toString());
        assertEquals("[3, 1, 2]", processor.eval("{3,1,2}").toString());
        assertEquals("{b=1, a=2}", processor.eval("{'b':1,'a':2}").toString());
    }

    @Test
    void theIndexOfANullBaseIsEvaluated() {
        // the section 1.6 makes the value of a null base null, and says nothing of the index, which is an
        // expression like any other
        assertThrows(ELException.class, () -> processor.eval("null[undefinedIdentifier]"));
        assertEquals((Object) 2L, processor.eval("y = 1; null[y = 2]; y"));
    }

    @Test
    void aSemicolonExpressionIsNotAnLvalue() {
        // the compiled path does not treat one as an lvalue and neither reference implementation does, so
        // resolving one would evaluate its left operand for nothing on every getType and isReadOnly
        ELContext context = new StandardELContext(factory);
        context.getVariableMapper().setVariable("bean", factory.createValueExpression(new Holder(), Object.class));
        ValueExpression expression =
            factory.createValueExpression(context, "${1 ; bean.name}", Object.class);
        assertTrue(expression.isReadOnly(context));
        assertNull(expression.getType(context));
        assertNull(expression.getValueReference(context));
        assertEquals("n", expression.getValue(context));
    }

    @Test
    void collectionConstruction() {
        assertEquals(List.of(1L, 2L, 3L), processor.eval("[1,2,3]"));
        assertEquals(Map.of("one", 1L), processor.eval("{'one':1}"));
        assertTrue(processor.eval("{1,2,3}") instanceof java.util.Set);
    }

    @Test
    void lambdaExpressions() {
        assertEquals((Object) 7L, processor.eval("((x,y)->x+y)(3,4)"));
        assertEquals((Object) 7L, processor.eval("v = (x,y)->x+y; v(3,4)"));
        assertEquals((Object) 120L, processor.eval("fact = n -> n==0? 1: n*fact(n-1); fact(5)"));
        assertEquals((Object) 3L, processor.eval("(x->y->x+y)(1)(2)"));
    }

    @Test
    void collectionOperations() {
        assertEquals(List.of(2L, 4L), processor.eval("[1,2,3,4].stream().filter(i->i mod 2 == 0).toList()"));
        assertEquals((Object) 10L, processor.eval("[1,2,3,4].stream().sum()"));
        assertEquals((Object) 4L, processor.eval("[1,2,3,4].stream().count()"));
        assertEquals(List.of(1L, 2L, 3L), processor.eval("[3,1,2].stream().sorted().toList()"));
        assertEquals((Object) 3L, processor.eval("[1,2,3].stream().max().get()"));
    }

    @Test
    void assignmentAndBeans() {
        processor.defineBean("greeting", "hello");
        assertEquals("hello", processor.eval("greeting"));
        assertEquals((Object) 5, processor.eval("greeting.length()"));
        assertEquals("HELLO", processor.eval("greeting.toUpperCase()"));
    }

    @Test
    void staticFieldAndConstructorReferences() {
        assertEquals(Boolean.TRUE, processor.eval("Boolean.TRUE"));
        assertEquals(Integer.valueOf(3), processor.eval("Integer.valueOf(3)"));
        assertEquals("x", processor.eval("String('x')"));
    }

    @Test
    void methodExpressionsResolveAnAccessibleDeclaration() {
        // List.of returns a class that is not public, the method must be invoked through the interface
        processor.defineBean("xs", List.of(1, 2, 3));
        jakarta.el.ELContext context = processor.getELManager().getELContext();
        jakarta.el.ExpressionFactory factory = jakarta.el.ExpressionFactory.newInstance();
        jakarta.el.MethodExpression size = factory.createMethodExpression(context, "#{xs.size}", Object.class, new Class<?>[0]);
        assertEquals((Object) 3, size.invoke(context, null));
        assertEquals("size", size.getMethodInfo(context).getName());
    }

    @Test
    void propertyMethodExpressionsUseTheResolverChain() {
        jakarta.el.ELContext context = processor.getELManager().getELContext();
        ExpressionFactory factory = ExpressionFactory.newInstance();
        MethodExpression valueOf = factory.createMethodExpression(context, "#{Integer.valueOf}", Integer.class,
            new Class<?>[]{String.class});

        assertEquals(42, valueOf.invoke(context, new Object[]{"42"}));
        assertEquals("valueOf", valueOf.getMethodInfo(context).getName());
        assertEquals("valueOf", valueOf.getMethodReference(context).getMethodInfo().getName());
    }

    @Test
    void propertyMethodExpressionsPackVarargsInTheReflectiveFallback() {
        ELContext context = new PropertyOnlyContext();
        MethodExpression join = ExpressionFactory.newInstance().createMethodExpression(context, "#{bean.join}",
            String.class, new Class<?>[]{String[].class});

        assertEquals("a,b", join.invoke(context, new Object[]{"a", "b"}));
    }

    @Test
    void functionsPackVariableArityArguments() throws NoSuchMethodException {
        processor.defineFunction("fn", "join", ELInterpreterTest.class.getMethod("join", String[].class));

        assertEquals("a,b", processor.eval("fn:join('a', 'b')"));
    }

    @Test
    void anIdentifierThatIsNotInvocableDefersToTheFunctionMapper() throws NoSuchMethodException {
        processor.defineBean("twice", 42L);
        processor.defineFunction("", "twice", ELInterpreterTest.class.getMethod("twice", long.class));
        assertEquals((Object) 6L, processor.eval("twice(3)"));
        assertThrows(jakarta.el.MethodNotFoundException.class, () -> processor.eval("twice2(3)"));
    }

    public static long twice(long value) {
        return value * 2;
    }

    /**
     * A bean with a writable property, so that an lvalue has something to resolve to.
     */
    public static final class Holder {

        private String name = "n";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static String join(String... values) {
        return String.join(",", values);
    }

    public static final class Varargs {
        public String join(String... values) {
            return String.join(",", values);
        }
    }

    private static final class PropertyOnlyContext extends ELContext {
        private final ELResolver resolver = new ELResolver() {
            @Override
            public Object getValue(ELContext context, Object base, Object property) {
                if (base == null && "bean".equals(property)) {
                    context.setPropertyResolved(true);
                    return new Varargs();
                }
                return null;
            }

            @Override
            public Class<?> getType(ELContext context, Object base, Object property) {
                return null;
            }

            @Override
            public void setValue(ELContext context, Object base, Object property, Object value) {
            }

            @Override
            public boolean isReadOnly(ELContext context, Object base, Object property) {
                return true;
            }

            @Override
            public Class<?> getCommonPropertyType(ELContext context, Object base) {
                return null;
            }
        };

        @Override
        public ELResolver getELResolver() {
            return resolver;
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return null;
        }

        @Override
        public VariableMapper getVariableMapper() {
            return null;
        }
    }
}
