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

import io.micronaut.core.annotation.Internal;
import io.micronaut.el.ELSandbox;
import io.micronaut.el.parser.ELParser;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.el.runtime.ELMethods;
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.MethodExpression;
import jakarta.el.MethodInfo;
import jakarta.el.MethodNotFoundException;
import jakarta.el.MethodReference;
import jakarta.el.PropertyNotFoundException;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * A {@link MethodExpression} evaluating the abstract syntax tree produced by the parser.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class InterpretedMethodExpression extends MethodExpression {

    private static final long serialVersionUID = 1L;

    private final String expressionString;
    private final Class<?> expectedReturnType;
    private final Class<?> @Nullable [] expectedParamTypes;
    private transient @Nullable ELNode node;
    private final transient ELNode.@Nullable Method invocation;
    private transient @Nullable ELInterpreter interpreter;

    InterpretedMethodExpression(String expressionString,
                                Class<?> expectedReturnType,
                                Class<?> @Nullable [] expectedParamTypes,
                                ELNode node,
                                ELInterpreter interpreter) {
        this.expressionString = Objects.requireNonNull(expressionString, "expressionString");
        this.expectedReturnType = Objects.requireNonNull(expectedReturnType, "expectedReturnType");
        this.expectedParamTypes = expectedParamTypes;
        this.node = Objects.requireNonNull(node, "node");
        this.invocation = unwrap(node) instanceof ELNode.Method method ? method : null;
        this.interpreter = Objects.requireNonNull(interpreter, "interpreter");
    }

    @Override
    @Nullable
    public Object invoke(ELContext context, @Nullable Object[] params) {
        context.notifyBeforeEvaluation(expressionString);
        Object result = doInvoke(context, params);
        context.notifyAfterEvaluation(expressionString);
        if (expectedReturnType == void.class) {
            return null;
        }
        Object coerced = ELSupport.coerceToType(context, result, expectedReturnType);
        return ELSandbox.checksResultOf(expectedReturnType) ? ELSandboxGuard.checkResult(context, coerced) : coerced;
    }

    @Override
    public MethodInfo getMethodInfo(ELContext context) {
        Method method = findMethod(context);
        return new MethodInfo(method.getName(), method.getReturnType(), method.getParameterTypes());
    }

    @Override
    public MethodReference getMethodReference(ELContext context) {
        ELNode.Method providedInvocation = invocation;
        ELInterpreter.Target target = interpreter().resolveTarget(context,
            providedInvocation == null ? node() : methodTargetNode());
        Object base = target == null ? null : target.base();
        Object[] arguments = providedInvocation == null
            ? null
            : interpreter().evaluateArguments(context, providedInvocation.arguments());
        Method method = findMethod(base, target == null ? null : target.property(), arguments);
        MethodInfo methodInfo = new MethodInfo(method.getName(), method.getReturnType(), method.getParameterTypes());
        return new MethodReference(base, methodInfo, method.getAnnotations(), arguments);
    }

    @Override
    public boolean isParametersProvided() {
        return invocation != null;
    }

    @Override
    public String getExpressionString() {
        return expressionString;
    }

    @Override
    public boolean isLiteralText() {
        return false;
    }

    private ELNode node() {
        ELNode resolved = node;
        if (resolved == null) {
            resolved = ELParser.parse(expressionString);
            node = resolved;
        }
        return resolved;
    }

    private ELInterpreter interpreter() {
        ELInterpreter resolved = interpreter;
        if (resolved == null) {
            resolved = ELInterpreter.of(null, node());
            interpreter = resolved;
        }
        return resolved;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof InterpretedMethodExpression other
            && other.node().equals(node())
            && other.expectedReturnType.equals(expectedReturnType)
            && Arrays.equals(other.expectedParamTypes, expectedParamTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node(), expectedReturnType, Arrays.hashCode(expectedParamTypes));
    }

    @Override
    public String toString() {
        return "MethodExpression[" + expressionString + "]";
    }

    @Nullable
    private Object doInvoke(ELContext context, @Nullable Object[] params) {
        if (invocation != null) {
            // the expression provides its own parameters, the ones passed to invoke are ignored
            return interpreter().evaluate(context, node());
        }
        ELInterpreter.Target target = interpreter().resolveTarget(context, node());
        if (target == null) {
            throw new MethodNotFoundException("The expression '" + expressionString + "' is not a method expression");
        }
        if (target.base() == null) {
            Object identifier = ELSandboxGuard.resolveIdentifier(context, ELSupport.coerceToString(target.property()));
            return ELResolution.invokeMethodExpression(context, identifier, params);
        }
        Object base = target.base();
        ELSandboxGuard.check(context, base, target.property());
        try {
            return ELResolution.invokeWithParamTypes(context, base, target.property(), expectedParamTypes, params);
        } catch (MethodNotFoundException ignored) {
            // Some ELContext implementations provide a resolver for the base's properties but not its methods.
            return ELResolution.invokeMethod(context, base, findMethod(base, target.property(), params), params);
        }
    }

    private ELNode methodTargetNode() {
        ELNode.Method providedInvocation = invocation;
        return providedInvocation == null
            ? node()
            : new ELNode.Property(providedInvocation.base(), providedInvocation.property());
    }

    private Method findMethod(ELContext context) {
        ELNode.Method providedInvocation = invocation;
        if (providedInvocation == null) {
            ELInterpreter.Target target = interpreter().resolveTarget(context, node());
            return findMethod(target == null ? null : target.base(),
                target == null ? null : target.property(), null);
        }
        ELInterpreter.Target target = interpreter().resolveTarget(context, methodTargetNode());
        Object[] arguments = interpreter().evaluateArguments(context, providedInvocation.arguments());
        return findMethod(target == null ? null : target.base(),
            target == null ? null : target.property(), arguments);
    }

    private Method findMethod(@Nullable Object base,
                              @Nullable Object property,
                              Object @Nullable [] arguments) {
        if (base == null) {
            throw new PropertyNotFoundException("Cannot resolve the base object of the expression '"
                + expressionString + "'");
        }
        if (property == null) {
            throw new MethodNotFoundException("Cannot resolve the method of the expression '"
                + expressionString + "'");
        }
        String name = ELSupport.coerceToString(property);
        Class<?>[] paramTypes = arguments == null ? expectedParamTypes : null;
        return base instanceof ELClass elClass
            ? ELMethods.findStaticMethod(elClass.getKlass(), name, paramTypes, arguments)
            : ELMethods.findMethod(base.getClass(), name, paramTypes, arguments);
    }

    private static ELNode unwrap(ELNode node) {
        return node instanceof ELNode.Eval eval ? unwrap(eval.expression()) : node;
    }
}
