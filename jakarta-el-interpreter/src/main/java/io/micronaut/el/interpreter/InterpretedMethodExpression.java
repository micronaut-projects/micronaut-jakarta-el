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
import io.micronaut.core.type.Argument;
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELSandbox;
import io.micronaut.el.parser.ELParser;
import io.micronaut.el.parser.ELNodes;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.el.runtime.ELSupport;
import io.micronaut.el.runtime.ELExpressionIdentity;
import io.micronaut.el.runtime.ELVariableBindings;
import jakarta.el.ELContext;
import jakarta.el.MethodExpression;
import jakarta.el.MethodInfo;
import jakarta.el.MethodNotFoundException;
import jakarta.el.MethodReference;
import jakarta.el.PropertyNotFoundException;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * A {@link MethodExpression} evaluating the abstract syntax tree produced by the parser.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class InterpretedMethodExpression extends MethodExpression implements ELExpressionIdentity {

    private static final long serialVersionUID = 1L;

    private final String expressionString;
    private final Class<?> expectedReturnType;
    private final Class<?> @Nullable [] expectedParamTypes;
    private final Map<String, ELMethod> functions;
    private transient @Nullable ELNode node;
    private transient ELNode.@Nullable Method invocation;
    private transient @Nullable ELInterpreter interpreter;
    private transient @Nullable String equalityForm;

    InterpretedMethodExpression(String expressionString,
                                Class<?> expectedReturnType,
                                Class<?> @Nullable [] expectedParamTypes,
                                ELNode node,
                                Map<String, ELMethod> functions,
                                ELInterpreter interpreter) {
        this.expressionString = Objects.requireNonNull(expressionString, "expressionString");
        this.expectedReturnType = Objects.requireNonNull(expectedReturnType, "expectedReturnType");
        this.expectedParamTypes = expectedParamTypes == null ? null : expectedParamTypes.clone();
        this.functions = Map.copyOf(functions);
        this.node = Objects.requireNonNull(node, "node");
        this.invocation = unwrap(node) instanceof ELNode.Method method ? method : null;
        this.interpreter = Objects.requireNonNull(interpreter, "interpreter");
    }

    @Override
    @Nullable
    public Object invoke(ELContext context, Object @Nullable [] params) {
        context.notifyBeforeEvaluation(expressionString);
        Object result = doInvoke(context, params);
        Object coerced = expectedReturnType == void.class ? null : ELSupport.coerceToType(context, result, expectedReturnType);
        context.notifyAfterEvaluation(expressionString);
        return ELSandbox.checksResultOf(expectedReturnType) ? ELSandboxGuard.checkResult(context, coerced) : coerced;
    }

    @Override
    public MethodInfo getMethodInfo(ELContext context) {
        MethodExpression identifier = identifierMethodExpression(context);
        if (identifier != null) {
            return identifier.getMethodInfo(context);
        }
        ELNode.Method providedInvocation = invocation();
        ELInterpreter.Target target = interpreter().resolveTarget(context,
            providedInvocation == null ? node() : methodTargetNode());
        Object[] arguments = providedInvocation == null
            ? null
            : interpreter().evaluateArguments(context, providedInvocation.arguments());
        ELMethod method = findMethod(context, target, arguments);
        return methodInfo(method);
    }

    @Override
    public MethodReference getMethodReference(ELContext context) {
        context.notifyBeforeEvaluation(expressionString);
        MethodExpression identifier = identifierMethodExpression(context);
        if (identifier != null) {
            MethodReference reference = identifier.getMethodReference(context);
            context.notifyAfterEvaluation(expressionString);
            return reference;
        }
        ELNode.Method providedInvocation = invocation();
        ELInterpreter.Target target = interpreter().resolveTarget(context,
            providedInvocation == null ? node() : methodTargetNode());
        Object base = target == null ? null : target.base();
        Object[] arguments = providedInvocation == null
            ? null
            : interpreter().evaluateArguments(context, providedInvocation.arguments());
        ELMethod method = findMethod(context, target, arguments);
        MethodInfo methodInfo = methodInfo(method);
        MethodReference reference = new MethodReference(base, methodInfo, method.synthesizeAnnotations(), arguments);
        context.notifyAfterEvaluation(expressionString);
        return reference;
    }

    @Override
    public boolean isParametersProvided() {
        return invocation() != null;
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
            resolved = ELInterpreter.of(functions);
            interpreter = resolved;
        }
        return resolved;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        Object unwrapped = obj instanceof MethodExpression expression ? ELVariableBindings.unwrap(expression) : obj;
        return unwrapped instanceof MethodExpression
            && unwrapped instanceof ELExpressionIdentity identity
            && identity.equalityForm().equals(equalityForm());
    }

    @Override
    public int hashCode() {
        return equalityForm().hashCode();
    }

    @Override
    public String equalityForm() {
        String resolved = equalityForm;
        if (resolved == null) {
            resolved = ELNodes.canonical(node(), (prefix, localName) -> {
                ELMethod function = functions.get(
                    prefix.isEmpty() ? localName : prefix + ":" + localName);
                return function == null ? null : function.identity();
            });
            equalityForm = resolved;
        }
        return resolved;
    }

    @Override
    public String toString() {
        return "MethodExpression[" + expressionString + "]";
    }

    @Nullable
    private Object doInvoke(ELContext context, Object @Nullable [] params) {
        if (invocation() != null) {
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
        return ELInterpreter.invokeWithParams(context, executors(), base, target.property(), expectedParamTypes, params);
    }

    private ELNode methodTargetNode() {
        ELNode.Method providedInvocation = invocation();
        return providedInvocation == null
            ? node()
            : new ELNode.Property(providedInvocation.base(), providedInvocation.property());
    }

    @Nullable
    private MethodExpression identifierMethodExpression(ELContext context) {
        if (invocation() != null) {
            return null;
        }
        ELInterpreter.Target target = interpreter().resolveTarget(context, node());
        if (target == null || target.base() != null) {
            return null;
        }
        Object identifier = ELResolution.resolveIdentifier(context, ELSupport.coerceToString(target.property()));
        if (identifier instanceof MethodExpression expression) {
            return expression;
        }
        throw new MethodNotFoundException("The expression '" + expressionString + "' does not resolve to a method expression");
    }

    private ELMethod findMethod(ELContext context,
                                ELInterpreter.@Nullable Target target,
                                Object @Nullable [] arguments) {
        if (target == null || target.base() == null) {
            throw new PropertyNotFoundException("Cannot resolve the base object of the expression '"
                + expressionString + "'");
        }
        if (target.property() == null) {
            throw new MethodNotFoundException("Cannot resolve the method of the expression '"
                + expressionString + "'");
        }
        Class<?>[] paramTypes = arguments == null ? expectedParamTypes : null;
        return ELInterpreter.resolveMethod(context, executors(), target.base(), target.property(), paramTypes, arguments);
    }

    private java.util.List<io.micronaut.el.ELMethodExecutor> executors() {
        return interpreter().executors();
    }

    private ELNode.@Nullable Method invocation() {
        ELNode.Method resolved = invocation;
        if (resolved == null && unwrap(node()) instanceof ELNode.Method method) {
            resolved = method;
            invocation = resolved;
        }
        return resolved;
    }

    private static ELNode unwrap(ELNode node) {
        return node instanceof ELNode.Eval eval ? unwrap(eval.expression()) : node;
    }

    private static MethodInfo methodInfo(ELMethod method) {
        return new MethodInfo(method.getName(), method.getReturnType().getType(),
            Argument.toClassArray(method.getArguments()));
    }
}
