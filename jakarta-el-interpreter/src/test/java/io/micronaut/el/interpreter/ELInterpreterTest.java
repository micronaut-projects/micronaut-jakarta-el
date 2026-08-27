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

import jakarta.el.ELProcessor;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodExpression;
import jakarta.el.VariableMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ELInterpreterTest {

    private final ELProcessor processor = new ELProcessor();

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
    void conditionalAndSemicolonOperators() {
        assertEquals("yes", processor.eval("true ? 'yes' : 'no'"));
        assertEquals((Object) 2L, processor.eval("1; 2"));
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
        processor.defineFunction("fn", "join", Varargs.class.getMethod("join", CharSequence[].class));

        assertEquals("a,b", processor.eval("fn:join('a', 'b')"));
    }

    @Test
    void constructorsPackVariableArityArguments() {
        ELContext context = processor.getELManager().getELContext();
        context.getImportHandler().importClass(Varargs.class.getName());

        assertEquals("a,b", processor.eval("Varargs('a', 'b').value"));
    }

    @Test
    void variableArityMethodsHandleDirectArrays() {
        processor.defineBean("varargs", new Varargs());
        processor.defineBean("strings", new String[]{"a", "b"});

        assertEquals("1:int[]", processor.eval("varargs.argumentType(varargs.numbers)"));
        assertEquals("a,b", processor.eval("varargs.join(strings)"));
    }

    @Test
    void lambdasCoerceToUnannotatedFunctionalInterfaces() {
        processor.defineBean("functions", new InterpreterFunctions());

        assertEquals("EL", processor.eval("functions.map(value -> value.toUpperCase(), 'el')"));
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
