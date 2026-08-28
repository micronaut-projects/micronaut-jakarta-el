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
import jakarta.el.ELException;
import jakarta.el.EvaluationListener;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodExpression;
import jakarta.el.VariableMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

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
        assertEquals((Object) false, processor.eval("null < (-1 < '1.5')"));
        assertEquals((Object) false, processor.eval("null > (-1 < '1.5')"));
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
        MethodExpression argumentType = ExpressionFactory.newInstance().createMethodExpression(context,
            "#{bean.argumentType}", String.class, new Class<?>[]{Object[].class});

        assertEquals("a,b", join.invoke(context, new Object[]{"a", "b"}));
        assertEquals("1:java.lang.String[]",
            argumentType.invoke(context, new Object[]{new String[]{"a", "b"}}));
    }

    @Test
    void functionsPackVariableArityArguments() throws NoSuchMethodException {
        processor.defineFunction("fn", "join", Varargs.class.getMethod("join", CharSequence[].class));
        processor.defineBean("sequences", new CharSequence[]{"a", "b"});
        processor.defineBean("strings", new String[]{"a", "b"});

        assertEquals("a,b", processor.eval("fn:join('a', 'b')"));
        assertEquals("a,b", processor.eval("fn:join(sequences)"));
        assertEquals("a,b", processor.eval("fn:join(strings)"));
    }

    @Test
    void unqualifiedFunctionsPackVariableArityArguments() throws NoSuchMethodException {
        processor.defineFunction("", "join", Varargs.class.getMethod("join", CharSequence[].class));

        assertEquals("a,b", processor.eval("join('a', 'b')"));
    }

    @Test
    void functionsRejectTooFewFixedArguments() throws NoSuchMethodException {
        processor.defineFunction("fn", "combine", Varargs.class.getMethod("combine", String.class, String[].class));

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
            () -> processor.eval("fn:combine()"));
        assertTrue(failure.getMessage().contains("at least 1 argument"), failure.getMessage());
    }

    @Test
    void constructorsPackVariableArityArguments() {
        ELContext context = processor.getELManager().getELContext();
        context.getImportHandler().importClass(Varargs.class.getName());
        context.getImportHandler().importClass(VarargsConstructor.class.getName());

        assertEquals("a,b", processor.eval("Varargs('a', 'b').value"));
        assertEquals("a,b", processor.eval("VarargsConstructor('a', 'b').value"));
    }

    @Test
    void variableArityMethodsHandleDirectArrays() {
        processor.defineBean("varargs", new Varargs());
        processor.defineBean("strings", new String[]{"a", "b"});

        assertEquals("1:int[]", processor.eval("varargs.argumentType(varargs.numbers)"));
        assertEquals("a,b", processor.eval("varargs.join(strings)"));
        assertEquals("a", processor.eval("strings[0]"));
    }

    @Test
    void lambdasCoerceToUnannotatedFunctionalInterfaces() {
        processor.defineBean("functions", new InterpreterFunctions());
        processor.defineBean("f", new InterpreterFunctions());

        assertEquals("default:EL", processor.eval("functions.map(value -> value.toUpperCase(), 'el')"));
        assertEquals("default:EL", processor.eval("f.map(value -> value.toUpperCase(), 'el')"));
    }

    @Test
    void generatedLvalueRegressionsHaveExactInterpretedCounterparts() {
        processor.defineBean("book", new InterpreterBook());
        processor.defineBean("item", new InterpreterBook());
        processor.defineBean("formatting", new InterpreterFunctions());
        processor.defineBean("xs", List.of(1, 2, 3));
        processor.defineBean("strings", new String[]{"a", "b"});
        ELContext context = processor.getELManager().getELContext();
        ExpressionFactory factory = ExpressionFactory.newInstance();

        assertEquals("1:int[]", processor.eval("formatting.argumentType(formatting.numbers)"));
        assertEquals("1:java.lang.String[]", processor.eval("formatting.argumentType(strings)"));
        assertEquals((Object) 7, processor.eval("Math.max(item.quantity, Integer.valueOf(7))"));
        assertEquals(25d, processor.eval("Math.max(book.unitPrice, 25.0)"));

        MethodExpression selectInteger = factory.createMethodExpression(context, "${formatting.select}",
            String.class, new Class<?>[]{Integer.class});
        MethodExpression selectString = factory.createMethodExpression(context, "${formatting.select}",
            String.class, new Class<?>[]{String.class});
        MethodExpression valueOf = factory.createMethodExpression(context, "${Integer.valueOf}", Integer.class,
            new Class<?>[]{String.class});
        MethodExpression size = factory.createMethodExpression(context, "${xs.size}", Integer.class,
            new Class<?>[0]);
        MethodExpression discounted = factory.createMethodExpression(context, "${book.discounted(50)}",
            Double.class, null);
        MethodExpression describe = factory.createMethodExpression(context, "${book.describe}", String.class,
            new Class<?>[0]);

        assertEquals("integer", selectInteger.invoke(context, new Object[]{"1"}));
        assertEquals("string", selectString.invoke(context, new Object[]{"1"}));
        assertEquals(42, valueOf.invoke(context, new Object[]{"42"}));
        assertEquals(3, size.invoke(context, null));
        assertEquals(10d, discounted.invoke(context, null));
        assertEquals("EL (history)", describe.invoke(context, null));
    }

    @Test
    void overloadSelectionUsesJakartaElPrecedenceAndSpecificity() {
        processor.defineBean("varargs", new Varargs());
        processor.defineBean("integer", 1);

        assertEquals("assignable", processor.eval("varargs.choose(1, 1)"));
        assertEquals("number", processor.eval("varargs.specific(1)"));
        assertEquals("wrapper", processor.eval("varargs.boxed(integer)"));
        assertThrows(jakarta.el.MethodNotFoundException.class,
            () -> processor.eval("varargs.emptyVarargs()"));
        assertThrows(jakarta.el.MethodNotFoundException.class,
            () -> processor.eval("varargs.numeric(1)"));
        assertThrows(jakarta.el.MethodNotFoundException.class,
            () -> processor.eval("varargs.reject(value -> value)"));
        assertThrows(jakarta.el.MethodNotFoundException.class,
            () -> processor.eval("varargs.rejectSealed(value -> value)"));
    }

    @Test
    void declaredParameterTypesSelectCompatibleAndExpandedVarargsMethods() {
        processor.defineBean("varargs", new Varargs());
        ELContext context = processor.getELManager().getELContext();
        ExpressionFactory factory = ExpressionFactory.newInstance();

        MethodExpression compatible = factory.createMethodExpression(context, "#{varargs.compatible}", String.class,
            new Class<?>[]{String.class});
        MethodExpression expanded = factory.createMethodExpression(context, "#{varargs.expanded}", String.class,
            new Class<?>[]{String.class, String.class});

        assertEquals("String", compatible.invoke(context, new Object[]{"value"}));
        assertEquals("a,b", expanded.invoke(context, new Object[]{"a", "b"}));
    }

    @Test
    void compileTimeRejectionsHaveInterpretedContracts() {
        ELContext context = processor.getELManager().getELContext();
        context.getImportHandler().importStatic(AmbiguousFunctions.class.getName() + ".ambiguous");

        // Static selection is provably ambiguous, so a generated expression must reject it during compilation.
        assertThrows(jakarta.el.MethodNotFoundException.class, () -> processor.eval("ambiguous('x')"));
        // A method expression must be a single eval-expression, so it cannot be generated for composite text.
        assertThrows(ELException.class, () -> ExpressionFactory.newInstance()
            .createMethodExpression(context, "Hello ${book.describe}", Object.class, new Class<?>[0]));
    }

    @Test
    void identifierMethodExpressionsDelegateMetadata() {
        ELContext context = processor.getELManager().getELContext();
        processor.defineBean("xs", List.of(1, 2, 3));
        MethodExpression target = ExpressionFactory.newInstance().createMethodExpression(context,
            "#{xs.size}", Object.class, new Class<?>[0]);
        processor.defineBean("target", target);
        MethodExpression expression = ExpressionFactory.newInstance().createMethodExpression(context,
            "#{target}", Object.class, new Class<?>[0]);

        assertEquals(3, expression.invoke(context, null));
        assertEquals("size", expression.getMethodInfo(context).getName());
        assertEquals("size", expression.getMethodReference(context).getMethodInfo().getName());
    }

    @Test
    void declaredParameterTypesAreDefensivelyCopied() {
        processor.defineBean("varargs", new Varargs());
        ELContext context = processor.getELManager().getELContext();
        Class<?>[] parameterTypes = {Number.class};
        MethodExpression expression = ExpressionFactory.newInstance().createMethodExpression(context,
            "#{varargs.specific}", String.class, parameterTypes);
        int hash = expression.hashCode();

        parameterTypes[0] = Object.class;

        assertEquals("number", expression.invoke(context, new Object[]{1L}));
        assertEquals(hash, expression.hashCode());
    }

    @Test
    void listenersObserveMethodReferencesAndCompletedCoercions() {
        List<String> events = new ArrayList<>();
        processor.getELManager().addELResolver(new FailingIntegerConversionResolver(events));
        ELContext context = processor.getELManager().getELContext();
        context.addEvaluationListener(new RecordingListener(events));
        processor.defineBean("varargs", new Varargs());

        MethodExpression reference = ExpressionFactory.newInstance().createMethodExpression(context,
            "#{varargs.numberText}", String.class, new Class<?>[0]);
        reference.getMethodReference(context);
        assertEquals(List.of("before:#{varargs.numberText}", "after:#{varargs.numberText}"), events);

        events.clear();
        assertThrows(ELException.class, () -> ExpressionFactory.newInstance()
            .createValueExpression(context, "${'1'}", Integer.class).getValue(context));
        assertEquals(List.of("before:${'1'}", "coerce"), events);

        events.clear();
        MethodExpression method = ExpressionFactory.newInstance().createMethodExpression(context,
            "#{varargs.numberText}", Integer.class, new Class<?>[0]);
        assertThrows(ELException.class, () -> method.invoke(context, null));
        assertEquals(List.of("before:#{varargs.numberText}", "coerce"), events);
    }

    @Test
    void methodExpressionsWithProvidedParametersSurviveSerialization() throws Exception {
        ELContext context = processor.getELManager().getELContext();
        processor.defineBean("varargs", new Varargs());
        MethodExpression expression = ExpressionFactory.newInstance().createMethodExpression(context,
            "#{varargs.join('a', 'b')}", String.class, null);

        MethodExpression copy = roundTrip(expression);
        assertTrue(copy.isParametersProvided());
        assertEquals("a,b", copy.invoke(context, null));
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

    private static MethodExpression roundTrip(MethodExpression expression) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(expression);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (MethodExpression) input.readObject();
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

    private static final class RecordingListener extends EvaluationListener {
        private final List<String> events;

        private RecordingListener(List<String> events) {
            this.events = events;
        }

        @Override
        public void beforeEvaluation(ELContext context, String expression) {
            events.add("before:" + expression);
        }

        @Override
        public void afterEvaluation(ELContext context, String expression) {
            events.add("after:" + expression);
        }
    }

    private static final class FailingIntegerConversionResolver extends ELResolver {
        private final List<String> events;

        private FailingIntegerConversionResolver(List<String> events) {
            this.events = events;
        }

        @Override
        public <T> T convertToType(ELContext context, Object value, Class<T> type) {
            if (type == Integer.class) {
                context.setPropertyResolved(true);
                events.add("coerce");
                throw new ELException("conversion failed");
            }
            return null;
        }

        @Override
        public Object getValue(ELContext context, Object base, Object property) {
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
    }
}
