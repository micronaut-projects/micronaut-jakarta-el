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

import io.micronaut.core.annotation.Introspected;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.ELSandbox;
import io.micronaut.el.ELSandboxException;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An expression string built at runtime is untrusted input, and the specification resolves properties,
 * methods, static members and constructors dynamically. Where that resolution reflects, these are the paths
 * through which it would otherwise reach arbitrary Java; where it does not, the application described what it
 * reaches and the sandbox stays out of the way.
 */
class ELSandboxTest {

    private final ExpressionFactory factory = new CompiledExpressionFactory();

    @Test
    void theProcessIsOutOfReach() {
        assertDenied("${Runtime.getRuntime()}");
        assertDenied("${Runtime.getRuntime().exec('/usr/bin/true')}");
        assertDenied("${ProcessBuilder('sh','-c','id').start()}");
        assertDenied("${System.getProperty('user.home')}");
        assertDenied("${System.exit(0)}");
    }

    @Test
    void theClassOfAnObjectIsOutOfReach() {
        assertDenied("${bean.getClass()}");
        assertDenied("${bean.class}");
        assertDenied("${bean['getClass']()}");
        assertDenied("${bean.getClass().forName('java.lang.Runtime')}");
        assertDenied("${bean.getBytes().getClass()}");
        assertDenied("${Class.forName('java.lang.Runtime')}");
    }

    @Test
    void theClassLoadersAndTheThreadsAreOutOfReach() {
        assertDenied("${Thread.currentThread()}");
        assertDenied("${Thread.currentThread().getContextClassLoader()}");
        assertDenied("${bean.getClass().getClassLoader()}");
    }

    @Test
    void aDeniedTypeReachedThroughALambdaIsStillDenied() {
        assertDenied("${list.stream().map(x -> x.getClass()).toList()}");
        assertDenied("${list.stream().filter(x -> x.getClass() != null).toList()}");
    }

    @Test
    void aDeniedTypeIsDeniedThroughAnAssignmentAndThroughAMethodExpression() {
        CompiledELContext context = context().setBean("described", new DescribedHolder());
        assertDenied(context, "${described.type.name = 'x'}");
        MethodExpression expression =
            factory.createMethodExpression(context, "${bean.getClass}", Object.class, new Class<?>[0]);
        assertThrows(ELSandboxException.class, () -> expression.invoke(context, new Object[0]));
    }

    @Test
    void everyOperationOfAMethodExpressionOnADeniedBaseIsDeniedNotOnlyItsInvocation() {
        CompiledELContext context = context().setBean("described", new DescribedHolder());
        MethodExpression expression = factory.createMethodExpression(context, "${described.type.getName}",
            Object.class, new Class<?>[0]);

        assertThrows(ELSandboxException.class, () -> expression.invoke(context, new Object[0]));
        // the metadata of a method found reflectively is read reflectively, whichever way it is asked for
        assertThrows(ELSandboxException.class, () -> expression.getMethodInfo(context));
        assertThrows(ELSandboxException.class, () -> expression.getMethodReference(context));
    }

    @Test
    void aMethodReferenceDoesNotHandBackADeniedArgument() {
        ELContext context = context();
        ((CompiledELContext) context).setBean("holder", new Holder());
        MethodExpression expression = factory.createMethodExpression(context,
            "${list.contains(holder.type)}", Object.class, new Class<?>[0]);

        // the base and the method are both allowed, and the argument is read reflectively, which is where the
        // denied object is stopped
        assertThrows(ELSandboxException.class, () -> expression.getMethodReference(context));
    }

    @Test
    void aMethodReferenceDoesNotHandBackADeniedBase() {
        ELContext context = context();
        ((CompiledELContext) context).setBean("holder", new Holder());
        MethodExpression expression = factory.createMethodExpression(context, "${holder.type.getName}",
            Object.class, new Class<?>[0]);

        // getMethodReference carries the base object, so a denied one must not travel out through it
        assertThrows(ELSandboxException.class, () -> expression.getMethodReference(context));
    }

    @Test
    void everyOperationOfAnExpressionThatReachesADeniedTypeIsDenied() {
        // getValueReference names the base and the property of an lvalue, and the base is read reflectively
        // like it is for getType and isReadOnly, so no operation is the one that hands it back
        ELContext context = context().setBean("holder", new Holder());
        ValueExpression expression =
            factory.createValueExpression(context, "${holder.type.name}", Object.class);
        assertThrows(ELSandboxException.class, () -> expression.getValue(context));
        assertThrows(ELSandboxException.class, () -> expression.getValueReference(context));
        assertThrows(ELSandboxException.class, () -> expression.getType(context));
        assertThrows(ELSandboxException.class, () -> expression.isReadOnly(context));
    }

    @Test
    void aDeniedTypeReflectionProducedIsNotHandedBack() {
        // the bean has no introspection, so its property is read reflectively and the value is checked where the
        // read produced it: whatever the expression goes on to do with it, returning it included
        ELContext context = context().setBean("holder", new Holder());
        ValueExpression asObject = factory.createValueExpression(context, "${holder.type}", Object.class);
        assertThrows(ELSandboxException.class, () -> asObject.getValue(context));
        MethodExpression method =
            factory.createMethodExpression(context, "${holder.getType}", Object.class, new Class<?>[0]);
        assertThrows(ELSandboxException.class, () -> method.invoke(context, new Object[0]));
        ValueExpression asString = factory.createValueExpression(context, "${holder.type}", String.class);
        assertThrows(ELSandboxException.class, () -> asString.getValue(context));
        assertDenied(context, "${[holder.type]}");
    }

    @Test
    void whatTheApplicationDescribedIsNotSandboxed() {
        // a bean introspection, a map and a list lead only where the application chose to lead: nothing is
        // reflected on, so the sandbox is not consulted even for a type it denies
        CompiledELContext context = context()
            .setBean("described", new DescribedHolder())
            .setBean("types", new LinkedHashMap<>(Map.of("class", String.class)))
            .setBean("typeList", List.of(String.class));
        assertEquals(String.class, evaluate(context, "${described.type}"));
        assertEquals(List.of(String.class), evaluate(context, "${[described.type]}"));
        assertEquals(String.class, evaluate(context, "${types['class']}"));
        assertEquals(String.class, evaluate(context, "${types.class}"));
        assertEquals(String.class, evaluate(context, "${typeList[0]}"));
    }

    @Test
    void aDescribedDeniedTypeIsStillDeniedToReflection() {
        // the introspection hands the class over, and reading anything of the class itself is reflection
        CompiledELContext context = context().setBean("described", new DescribedHolder());
        assertDenied(context, "${described.type.name}");
        assertDenied(context, "${described.type.getName()}");
        assertDenied(context, "${described.type.forName('java.lang.Runtime')}");
    }

    @Test
    void aContextWhoseResolverTheModuleDidNotBuildIsCheckedOnEveryProperty() {
        // what another resolver does cannot be told apart from reflection, so every property it resolves is
        // treated as a reflective one; the methods still go through the executors of the interpreter
        ELManager manager = new ELManager();
        manager.defineBean("bean", "hello");
        manager.defineBean("types", new LinkedHashMap<>(Map.of("k", String.class)));
        ELContext context = manager.getELContext();
        assertDenied(context, "${bean.class}");
        assertDenied(context, "${types.k}");
        assertEquals("HELLO", evaluate(context, "${bean.toUpperCase()}"));
    }

    @Test
    void thePropertyOfTheValueAnOptionalHoldsIsResolvedUnderTheSandbox() {
        // the resolver of the specification hands the property of the value on to the resolver of the context,
        // which must not be where the sandbox is left behind
        assertDenied(context().setBean("optional", Optional.of("hello")), "${optional.class}");
        CompiledELContext unrestricted = unrestricted(context().setBean("optional", Optional.of("hello")));
        assertEquals(String.class, evaluate(unrestricted, "${optional.class}"));
    }

    @Test
    void theFieldOfAStaticImportIsReadUnderTheSandbox() {
        CompiledELContext context = context();
        context.getImportHandler().importStatic("java.lang.Integer.TYPE");
        assertDenied(context, "${TYPE}");
        CompiledELContext unrestricted = unrestricted(context());
        unrestricted.getImportHandler().importStatic("java.lang.Integer.TYPE");
        assertEquals(int.class, evaluate(unrestricted, "${TYPE}"));
    }

    @Test
    void aFunctionTheFunctionMapperBindsIsInvokedUnderTheSandbox() throws NoSuchMethodException {
        // the reflective executor binds the functions of the mapper and invokes them reflectively
        Method runtime = Runtime.class.getMethod("getRuntime");
        assertDenied(runtimeFunctionContext(runtime), "${rt:runtime()}");
        CompiledELContext unrestricted = unrestricted(runtimeFunctionContext(runtime));
        assertSame(Runtime.getRuntime(), evaluate(unrestricted, "${rt:runtime()}"));
    }

    @Test
    void aSubclassOfADeniedTypeIsDenied() {
        assertFalse(ELSandbox.standard().allowsType(SecureClassLoaderSubclass.class));
        assertFalse(ELSandbox.standard().allowsType(java.lang.reflect.Method.class));
        assertFalse(ELSandbox.standard().allowsType(Class[].class));
    }

    @Test
    void theLanguageItselfIsUntouched() {
        assertEquals((Object) 2L, evaluate("${1 + 1}"));
        assertEquals("HELLO", evaluate("${bean.toUpperCase()}"));
        assertEquals((Object) 5, evaluate("${bean.length()}"));
        assertEquals((Object) 7, evaluate("${Integer.valueOf('7')}"));
        assertEquals((Object) Integer.MAX_VALUE, evaluate("${Integer.MAX_VALUE}"));
        assertEquals((Object) 2L, evaluate("${Math.max(1,2)}"));
        assertEquals(List.of(2L, 4L, 6L), evaluate("${list.stream().map(x -> x * 2).toList()}"));
        assertEquals((Object) 6L, evaluate("${list.stream().sum()}"));
        assertEquals("v", evaluate("${map['k']}"));
        assertEquals((Object) 1L, evaluate("${list[0]}"));
        assertEquals((Object) 6L, evaluate("${x = 3; x * 2}"));
        assertEquals("ab", evaluate("${'a' += 'b'}"));
    }

    @Test
    void aTypeOfTheApplicationIsNotDeniedForLivingUnderComSun() {
        // com.sun is not reserved for the platform: the technology compatibility kit publishes its own beans
        // under it, and so do applications
        assertTrue(ELSandbox.standard().allowsType(com.sun.example.Bean.class));
    }

    @Test
    void theSandboxOfTheContextReplacesTheStandardOne() {
        CompiledELContext context = unrestricted(context());
        assertEquals("java.lang.String", evaluate(context, "${bean.getClass().getName()}"));
    }

    @Test
    void theSandboxIsReadFromTheContextOfEachEvaluation() {
        // the interpreter caches the syntax tree of an expression string and shares the evaluators compiled
        // from it, so a sandbox baked into them would leak from one context to the next
        CompiledELContext open = unrestricted(context());
        CompiledELContext standard = context();
        assertEquals("java.lang.String", evaluate(open, "${bean.getClass().getName()}"));
        ValueExpression shared =
            factory.createValueExpression(standard, "${bean.getClass().getName()}", Object.class);
        assertThrows(ELSandboxException.class, () -> shared.getValue(standard));
        assertEquals("java.lang.String", shared.getValue(open));
    }

    @Test
    void aCompiledExpressionIsNotSandboxed() {
        // the sandbox is applied by the interpreter, which only creates the expressions that were not
        // compiled: an expression declared with @ELExpression is source of the application, even where it
        // resolves a member reflectively
        CompiledELContext context = context();
        assertEquals("java.lang.String", new StringClassName().getValue(context));
    }

    private void assertDenied(String expression) {
        assertDenied(context(), expression);
    }

    private void assertDenied(ELContext context, String expression) {
        ValueExpression valueExpression = factory.createValueExpression(context, expression, Object.class);
        assertThrows(ELSandboxException.class, () -> valueExpression.getValue(context), expression);
    }

    @SuppressWarnings("unchecked")
    private <T> T evaluate(String expression) {
        return (T) evaluate(context(), expression);
    }

    @SuppressWarnings("unchecked")
    private <T> T evaluate(ELContext context, String expression) {
        return (T) factory.createValueExpression(context, expression, Object.class).getValue(context);
    }

    private CompiledELContext context() {
        return new CompiledELContext()
            .setBean("bean", "hello")
            .setBean("list", new ArrayList<>(List.of(1L, 2L, 3L)))
            .setBean("map", new LinkedHashMap<>(Map.of("k", "v")));
    }

    private static CompiledELContext unrestricted(CompiledELContext context) {
        context.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);
        return context;
    }

    private static CompiledELContext runtimeFunctionContext(Method runtime) {
        return new CompiledELContext() {
            @Override
            public FunctionMapper getFunctionMapper() {
                return new FunctionMapper() {
                    @Override
                    public Method resolveFunction(String prefix, String localName) {
                        return "rt".equals(prefix) && "runtime".equals(localName) ? runtime : null;
                    }
                };
            }
        };
    }

    private static final class SecureClassLoaderSubclass extends ClassLoader {
    }

    /**
     * A bean of the application exposing a denied type, which it did not describe: an expression reads it
     * reflectively.
     */
    public static final class Holder {

        public Class<?> getType() {
            return String.class;
        }
    }

    /**
     * The same bean described by a bean introspection: an expression reads it without reflection.
     */
    @Introspected
    public static final class DescribedHolder {

        public Class<?> getType() {
            return String.class;
        }
    }

    /**
     * What the generated code of a compiled expression does, which does not go through the sandbox.
     */
    private static final class StringClassName {

        String getValue(ELContext context) {
            Object bean = io.micronaut.el.runtime.ELResolution.resolveIdentifier(context, "bean");
            return (String) io.micronaut.el.runtime.ELResolution.invoke(context,
                io.micronaut.el.runtime.ELResolution.invoke(context, bean, "getClass"), "getName");
        }
    }
}
