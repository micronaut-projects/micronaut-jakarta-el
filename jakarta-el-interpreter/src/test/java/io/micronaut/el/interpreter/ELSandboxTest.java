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
import io.micronaut.el.ELSandboxException;
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An expression string built at runtime is untrusted input, and the specification resolves properties,
 * methods, static members and constructors dynamically. These are the paths through which it would otherwise
 * reach arbitrary Java.
 */
class ELSandboxTest {

    private final ExpressionFactory factory = new CompiledExpressionFactory();

    @Test
    void theProcessIsOutOfReach() {
        assertDenied("${Runtime}");
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
        assertDenied("${bean.class = 1}");
        ELContext context = context();
        MethodExpression expression =
            factory.createMethodExpression(context, "${bean.getClass}", Object.class, new Class<?>[0]);
        assertThrows(ELSandboxException.class, () -> expression.invoke(context, new Object[0]));
    }

    @Test
    void everyOperationOfAnExpressionThatReachesADeniedTypeIsDenied() {
        // getValueReference names the base and the property of an lvalue, the same access getType and
        // isReadOnly make, so it must not be the one operation that hands the base back
        ELContext context = context().setBean("holder", new Holder());
        ValueExpression expression =
            factory.createValueExpression(context, "${holder.type.name}", Object.class);
        assertThrows(ELSandboxException.class, () -> expression.getValue(context));
        assertThrows(ELSandboxException.class, () -> expression.getValueReference(context));
        assertThrows(ELSandboxException.class, () -> expression.getType(context));
        assertThrows(ELSandboxException.class, () -> expression.isReadOnly(context));
    }

    @Test
    void aDeniedTypeIsNotHandedBackAsTheValueOfTheExpression() {
        // a bean of the application can expose one, and reaching it is not the same as returning it
        ELContext context = context().setBean("holder", new Holder());
        ValueExpression asObject = factory.createValueExpression(context, "${holder.type}", Object.class);
        assertThrows(ELSandboxException.class, () -> asObject.getValue(context));
        MethodExpression method =
            factory.createMethodExpression(context, "${holder.getType}", Object.class, new Class<?>[0]);
        assertThrows(ELSandboxException.class, () -> method.invoke(context, new Object[0]));
        // coerced to a string nothing of the denied type escapes, so the coercion is what is checked
        assertEquals("class java.lang.String",
            factory.createValueExpression(context, "${holder.type}", String.class).getValue(context));
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
        CompiledELContext context = context();
        context.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);
        assertEquals("java.lang.String", evaluate(context, "${bean.getClass().getName()}"));
    }

    @Test
    void theSandboxIsReadFromTheContextOfEachEvaluation() {
        // the interpreter caches the syntax tree of an expression string and shares the evaluators compiled
        // from it, so a sandbox baked into them would leak from one context to the next
        CompiledELContext open = context();
        open.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);
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
        // compiled: an expression declared with @ELExpression is source of the application
        CompiledELContext context = context();
        assertEquals("java.lang.String", new StringClassName().getValue(context));
    }

    private void assertDenied(String expression) {
        ELContext context = context();
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

    private static final class SecureClassLoaderSubclass extends ClassLoader {
    }

    /**
     * A bean of the application exposing a denied type, which is the only way an expression reaches one.
     */
    public static final class Holder {

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
