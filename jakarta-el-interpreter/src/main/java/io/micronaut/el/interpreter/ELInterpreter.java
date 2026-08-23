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
import io.micronaut.el.parser.ast.BinaryOperator;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.el.runtime.ELArithmetic;
import io.micronaut.el.runtime.ELCollections;
import io.micronaut.el.runtime.ELLambdas;
import io.micronaut.el.runtime.ELLiterals;
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.FunctionMapper;
import jakarta.el.ImportHandler;
import jakarta.el.LambdaExpression;
import jakarta.el.MethodNotFoundException;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.ValueReference;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Evaluates the abstract syntax tree produced by the parser.
 *
 * <p>The interpreter delegates every operator, coercion and resolution to the same runtime as the expressions
 * generated at compilation time, so both share one definition of the semantics of the specification.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class ELInterpreter {

    private static final Object[] NO_ARGUMENTS = new Object[0];

    private final Map<ELNode.Function, Method> functions;

    private ELInterpreter(Map<ELNode.Function, Method> functions) {
        this.functions = functions;
    }

    /**
     * Creates an interpreter for a parsed expression, binding its functions against the given context.
     *
     * @param context The context the expression is created for, can be {@code null}
     * @param node    The parsed expression
     * @return The interpreter
     */
    static ELInterpreter of(@Nullable ELContext context, ELNode node) {
        return new ELInterpreter(bindFunctions(context, node));
    }

    /**
     * Resolves every function of a parsed expression against the function mapper of the context.
     *
     * <p>The specification binds functions when the expression is created, so a later change of the mapper
     * does not affect an expression that was already created.</p>
     *
     * @param context The context, can be {@code null}
     * @param node    The parsed expression
     * @return The bound functions
     */
    static Map<ELNode.Function, Method> bindFunctions(@Nullable ELContext context, ELNode node) {
        if (context == null) {
            return Map.of();
        }
        FunctionMapper functionMapper = context.getFunctionMapper();
        if (functionMapper == null) {
            return Map.of();
        }
        Map<ELNode.Function, Method> bindings = new IdentityHashMap<>();
        bindFunctions(functionMapper, node, bindings);
        return bindings;
    }

    private static void bindFunctions(FunctionMapper functionMapper,
                                      ELNode node,
                                      Map<ELNode.Function, Method> bindings) {
        if (node instanceof ELNode.Function function) {
            Method method = functionMapper.resolveFunction(function.prefix(), function.localName());
            if (method != null) {
                bindings.put(function, method);
            }
        }
        for (ELNode child : children(node)) {
            bindFunctions(functionMapper, child, bindings);
        }
    }

    @SuppressWarnings("java:S1541")
    private static List<ELNode> children(ELNode node) {
        return switch (node) {
            case ELNode.Composite composite -> composite.parts();
            case ELNode.Eval eval -> List.of(eval.expression());
            case ELNode.Function function -> function.invocations().stream().flatMap(List::stream).toList();
            case ELNode.Property property -> List.of(property.base(), property.property());
            case ELNode.Method method -> concat(List.of(method.base(), method.property()), method.arguments());
            case ELNode.Call call -> concat(List.of(call.target()), call.arguments());
            case ELNode.Unary unary -> List.of(unary.operand());
            case ELNode.Binary binary -> List.of(binary.left(), binary.right());
            case ELNode.Ternary ternary -> List.of(ternary.condition(), ternary.ifTrue(), ternary.ifFalse());
            case ELNode.Assign assign -> List.of(assign.target(), assign.value());
            case ELNode.Semicolon semicolon -> List.of(semicolon.left(), semicolon.right());
            case ELNode.Lambda lambda -> List.of(lambda.body());
            case ELNode.SetData setData -> setData.elements();
            case ELNode.ListData listData -> listData.elements();
            case ELNode.MapData mapData -> mapData.entries().stream()
                .flatMap(entry -> entry.value() == null
                    ? java.util.stream.Stream.of(entry.key())
                    : java.util.stream.Stream.of(entry.key(), entry.value()))
                .toList();
            default -> List.of();
        };
    }

    private static List<ELNode> concat(List<ELNode> first, List<ELNode> second) {
        List<ELNode> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    /**
     * Evaluates a node.
     *
     * @param context The context
     * @param node    The node
     * @return The result of the evaluation
     */
    @Nullable
    @SuppressWarnings("java:S1541")
    Object evaluate(ELContext context, ELNode node) {
        return switch (node) {
            case ELNode.Composite composite -> evaluateComposite(context, composite);
            case ELNode.LiteralText literalText -> literalText.text();
            case ELNode.Eval eval -> evaluate(context, eval.expression());
            case ELNode.NullLiteral ignored -> null;
            case ELNode.BooleanLiteral literal -> literal.value();
            case ELNode.IntegerLiteral literal -> ELLiterals.integerValue(literal.image());
            case ELNode.FloatingPointLiteral literal -> ELLiterals.floatingPointValue(literal.image());
            case ELNode.StringLiteral literal -> literal.value();
            case ELNode.Identifier identifier -> ELResolution.resolveIdentifier(context, identifier.name());
            case ELNode.Function function -> evaluateFunction(context, function);
            case ELNode.Property property ->
                ELResolution.getValue(context, evaluate(context, property.base()), evaluate(context, property.property()));
            case ELNode.Method method -> ELResolution.invokeWithParams(
                context,
                evaluate(context, method.base()),
                evaluate(context, method.property()),
                evaluateAll(context, method.arguments()));
            case ELNode.Call call ->
                ELResolution.invokeCallable(context, evaluate(context, call.target()), evaluateAll(context, call.arguments()));
            case ELNode.Unary unary -> evaluateUnary(context, unary);
            case ELNode.Binary binary -> evaluateBinary(context, binary);
            case ELNode.Ternary ternary -> ELSupport.toBoolean(evaluate(context, ternary.condition()))
                ? evaluate(context, ternary.ifTrue())
                : evaluate(context, ternary.ifFalse());
            case ELNode.Assign assign -> evaluateAssign(context, assign);
            case ELNode.Semicolon semicolon ->
                ELSupport.sequence(evaluate(context, semicolon.left()), evaluate(context, semicolon.right()));
            case ELNode.Lambda lambda ->
                ELLambdas.create(context, lambda.parameters(), lambdaContext -> evaluate(lambdaContext, lambda.body()));
            case ELNode.SetData setData -> ELCollections.set(evaluateAll(context, setData.elements()));
            case ELNode.ListData listData -> ELCollections.list(evaluateAll(context, listData.elements()));
            case ELNode.MapData mapData -> evaluateMap(context, mapData);
        };
    }

    /**
     * Resolves the base object and the property of the last resolution of an lvalue.
     *
     * @param context The context
     * @param node    The node
     * @return The target, or {@code null} when the node is not an lvalue
     */
    @Nullable
    Target resolveTarget(ELContext context, ELNode node) {
        return switch (node) {
            case ELNode.Eval eval -> resolveTarget(context, eval.expression());
            case ELNode.Identifier identifier -> new Target(null, identifier.name());
            case ELNode.Property property ->
                new Target(evaluate(context, property.base()), evaluate(context, property.property()));
            case ELNode.Semicolon semicolon -> {
                evaluate(context, semicolon.left());
                yield resolveTarget(context, semicolon.right());
            }
            default -> null;
        };
    }

    /**
     * @param context The context
     * @param node    The node
     * @return The value reference of an lvalue, {@code null} otherwise
     */
    @Nullable
    ValueReference valueReference(ELContext context, ELNode node) {
        Target target = resolveTarget(context, node);
        return target == null ? null : new ValueReference(target.base(), target.property());
    }

    private Object evaluateComposite(ELContext context, ELNode.Composite composite) {
        StringBuilder builder = new StringBuilder();
        for (ELNode part : composite.parts()) {
            builder.append(ELSupport.coerceToString(evaluate(context, part)));
        }
        return builder.toString();
    }

    private Object evaluateMap(ELContext context, ELNode.MapData mapData) {
        Map<Object, Object> map = new LinkedHashMap<>();
        for (ELNode.MapData.MapEntry entry : mapData.entries()) {
            ELNode value = entry.value();
            map.put(evaluate(context, entry.key()), value == null ? null : evaluate(context, value));
        }
        return map;
    }

    @Nullable
    private Object evaluateUnary(ELContext context, ELNode.Unary unary) {
        Object value = evaluate(context, unary.operand());
        return switch (unary.operator()) {
            case NEGATE -> ELArithmetic.negate(value);
            case NOT -> ELSupport.not(value);
            case EMPTY -> ELSupport.isEmpty(value);
        };
    }

    @Nullable
    private Object evaluateBinary(ELContext context, ELNode.Binary binary) {
        BinaryOperator operator = binary.operator();
        // the logical operators stop as soon as the result is known
        if (operator == BinaryOperator.AND) {
            return ELSupport.toBoolean(evaluate(context, binary.left()))
                && ELSupport.toBoolean(evaluate(context, binary.right()));
        }
        if (operator == BinaryOperator.OR) {
            return ELSupport.toBoolean(evaluate(context, binary.left()))
                || ELSupport.toBoolean(evaluate(context, binary.right()));
        }
        Object left = evaluate(context, binary.left());
        Object right = evaluate(context, binary.right());
        return switch (operator) {
            case ADD -> ELArithmetic.add(left, right);
            case SUBTRACT -> ELArithmetic.subtract(left, right);
            case MULTIPLY -> ELArithmetic.multiply(left, right);
            case DIVIDE -> ELArithmetic.divide(left, right);
            case MODULO -> ELArithmetic.mod(left, right);
            case CONCAT -> ELArithmetic.concat(left, right);
            case LESS_THAN -> ELSupport.lessThan(left, right);
            case GREATER_THAN -> ELSupport.greaterThan(left, right);
            case LESS_THAN_OR_EQUAL -> ELSupport.lessThanOrEqual(left, right);
            case GREATER_THAN_OR_EQUAL -> ELSupport.greaterThanOrEqual(left, right);
            case EQUAL -> ELSupport.equals(left, right);
            case NOT_EQUAL -> ELSupport.notEquals(left, right);
            case AND, OR -> throw new IllegalStateException("Handled above: " + operator);
        };
    }

    @Nullable
    private Object evaluateAssign(ELContext context, ELNode.Assign assign) {
        Target target = resolveTarget(context, assign.target());
        if (target == null) {
            throw new PropertyNotWritableException("The left side of the assignment is not an lvalue");
        }
        Object value = evaluate(context, assign.value());
        if (target.base() == null) {
            return ELResolution.assignIdentifier(context, ELSupport.coerceToString(target.property()), value);
        }
        return ELResolution.assignProperty(context, target.base(), target.property(), value);
    }

    /**
     * Evaluates {@code func(args)(args)} as described in the section 1.5.2 of the specification.
     */
    @Nullable
    private Object evaluateFunction(ELContext context, ELNode.Function function) {
        Object result = resolveFunctionTarget(context, function);
        List<List<ELNode>> invocations = function.invocations();
        // the first invocation is consumed by the resolution of the function itself
        for (int i = 1; i < invocations.size(); i++) {
            result = ELResolution.invokeCallable(context, result, evaluateAll(context, invocations.get(i)));
        }
        return result;
    }

    @Nullable
    private Object resolveFunctionTarget(ELContext context, ELNode.Function function) {
        List<ELNode> firstArguments = function.invocations().isEmpty() ? List.of() : function.invocations().get(0);
        String prefix = function.prefix();
        String localName = function.localName();
        Object identifier = prefix.isEmpty() ? resolveIdentifierOrNull(context, localName) : null;
        if (identifier instanceof LambdaExpression || identifier instanceof ELClass) {
            return ELResolution.invokeCallable(context, identifier, evaluateAll(context, firstArguments));
        }
        Method method = resolveMappedFunction(context, function);
        if (method != null) {
            return invokeStatic(context, method, evaluateAll(context, firstArguments));
        }
        if (prefix.isEmpty()) {
            ImportHandler importHandler = context.getImportHandler();
            if (importHandler != null) {
                Class<?> resolvedClass = importHandler.resolveClass(localName);
                if (resolvedClass != null) {
                    return ELResolution.newInstance(context, new ELClass(resolvedClass), evaluateAll(context, firstArguments));
                }
                Class<?> staticClass = importHandler.resolveStatic(localName);
                if (staticClass != null) {
                    return ELResolution.invokeWithParams(context, new ELClass(staticClass), localName,
                        evaluateAll(context, firstArguments));
                }
            }
        }
        if (identifier != null) {
            throw new MethodNotFoundException("The identifier '" + localName + "' does not evaluate to an invocable"
                + " value: " + identifier);
        }
        throw new MethodNotFoundException("Cannot resolve the function '" + qualifiedName(prefix, localName) + "'");
    }

    @Nullable
    private Method resolveMappedFunction(ELContext context, ELNode.Function function) {
        Method bound = functions.get(function);
        if (bound != null) {
            return bound;
        }
        FunctionMapper functionMapper = context.getFunctionMapper();
        if (functionMapper == null) {
            return null;
        }
        return functionMapper.resolveFunction(function.prefix(), function.localName());
    }

    @Nullable
    private Object invokeStatic(ELContext context, Method method, Object[] arguments) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] coerced = new Object[parameterTypes.length];
        if (method.isVarArgs()) {
            throw new ELException("Variable arity functions are not supported: " + method);
        }
        if (arguments.length != parameterTypes.length) {
            throw new IllegalArgumentException("The function '" + method.getName() + "' expects "
                + parameterTypes.length + " argument(s) but " + arguments.length + " were provided");
        }
        for (int i = 0; i < parameterTypes.length; i++) {
            coerced[i] = ELSupport.coerceToType(context, arguments[i], parameterTypes[i]);
        }
        try {
            return method.invoke(null, coerced);
        } catch (IllegalAccessException e) {
            throw new ELException("Cannot invoke the function '" + method.getName() + "'", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ELException elException) {
                throw elException;
            }
            throw new ELException("The function '" + method.getName() + "' failed", cause);
        }
    }

    @Nullable
    private Object resolveIdentifierOrNull(ELContext context, String name) {
        if (context.isLambdaArgument(name)) {
            return context.getLambdaArgument(name);
        }
        if (context.getVariableMapper() != null && context.getVariableMapper().resolveVariable(name) != null) {
            return context.getVariableMapper().resolveVariable(name).getValue(context);
        }
        context.setPropertyResolved(false);
        Object value = context.getELResolver().getValue(context, null, name);
        return context.isPropertyResolved() ? value : null;
    }

    Object[] evaluateArguments(ELContext context, List<ELNode> nodes) {
        return evaluateAll(context, nodes);
    }

    private Object[] evaluateAll(ELContext context, List<ELNode> nodes) {
        if (nodes.isEmpty()) {
            return NO_ARGUMENTS;
        }
        List<Object> values = new ArrayList<>(nodes.size());
        for (ELNode node : nodes) {
            values.add(evaluate(context, node));
        }
        return values.toArray();
    }

    private static String qualifiedName(String prefix, String localName) {
        return prefix.isEmpty() ? localName : prefix + ":" + localName;
    }

    /**
     * The base object and the property of the last resolution of an lvalue.
     *
     * @param base     The base object, {@code null} for an identifier
     * @param property The property
     */
    record Target(@Nullable Object base, @Nullable Object property) {
    }
}
