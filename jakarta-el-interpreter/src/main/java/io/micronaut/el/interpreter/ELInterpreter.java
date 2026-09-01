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
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.parser.ast.BinaryOperator;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.el.resolver.ELMethodDiagnostics;
import io.micronaut.el.runtime.ELArithmetic;
import io.micronaut.el.runtime.ELArguments;
import io.micronaut.el.runtime.ELCollections;
import io.micronaut.el.runtime.ELLambdas;
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ImportHandler;
import jakarta.el.LambdaExpression;
import jakarta.el.MethodNotFoundException;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.ValueReference;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
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

    private final List<ELMethodExecutor> executors;
    private final Map<String, ELMethod> functions;
    @Nullable
    private Evaluator root;

    private ELInterpreter(List<ELMethodExecutor> executors, Map<String, ELMethod> functions) {
        this.executors = orderExecutors(executors);
        this.functions = functions;
    }

    /**
     * Evaluates the expression, through the evaluators compiled from its tree on the first evaluation.
     *
     * @param context The context
     * @param node    The parsed expression
     * @return The result of the evaluation
     */
    @Nullable
    Object evaluateRoot(ELContext context, ELNode node) {
        Evaluator evaluator = root;
        if (evaluator == null) {
            evaluator = compile(node);
            root = evaluator;
        }
        return evaluator.evaluate(context);
    }

    @SuppressWarnings("java:S1541")
    Evaluator compile(ELNode node) {
        return switch (node.kind()) {
            case COMPOSITE -> {
                Evaluator[] parts = compileAll(((ELNode.Composite) node).parts());
                yield new Evaluator() {
                    @Override
                    Object evaluate(ELContext context) {
                        StringBuilder builder = new StringBuilder();
                        for (Evaluator part : parts) {
                            builder.append(ELSupport.coerceToString(part.evaluate(context)));
                        }
                        return builder.toString();
                    }
                };
            }
            case LITERAL_TEXT -> new Constant(((ELNode.LiteralText) node).text());
            case EVAL -> compile(((ELNode.Eval) node).expression());
            case NULL_LITERAL -> new Constant(null);
            case BOOLEAN_LITERAL -> new Constant(((ELNode.BooleanLiteral) node).value());
            case INTEGER_LITERAL -> new Constant(((ELNode.IntegerLiteral) node).value());
            case FLOATING_POINT_LITERAL -> new Constant(((ELNode.FloatingPointLiteral) node).value());
            case STRING_LITERAL -> new Constant(((ELNode.StringLiteral) node).value());
            case IDENTIFIER -> {
                String name = ((ELNode.Identifier) node).name();
                yield new Evaluator() {
                    @Override
                    @Nullable
                    Object evaluate(ELContext context) {
                        return ELSandboxGuard.resolveIdentifier(context, name);
                    }
                };
            }
            case FUNCTION -> {
                ELNode.Function function = (ELNode.Function) node;
                yield new Evaluator() {
                    @Override
                    @Nullable
                    Object evaluate(ELContext context) {
                        return evaluateFunction(context, function);
                    }
                };
            }
            case PROPERTY -> {
                ELNode.Property property = (ELNode.Property) node;
                Evaluator base = compile(property.base());
                Evaluator name = compile(property.property());
                yield new Evaluator() {
                    @Override
                    @Nullable
                    Object evaluate(ELContext context) {
                        Object evaluatedBase = base.evaluate(context);
                        return evaluatedBase == null ? null
                            : ELSandboxGuard.getValue(context, evaluatedBase, name.evaluate(context));
                    }
                };
            }
            case METHOD -> {
                ELNode.Method method = (ELNode.Method) node;
                Evaluator base = compile(method.base());
                Evaluator name = compile(method.property());
                Evaluator[] arguments = compileAll(method.arguments());
                MethodCallSite callSite = new MethodCallSite();
                yield new Evaluator() {
                    @Override
                    @Nullable
                    Object evaluate(ELContext context) {
                        Object evaluatedBase = base.evaluate(context);
                        if (evaluatedBase == null) {
                            return null;
                        }
                        Object evaluatedName = name.evaluate(context);
                        return evaluatedName == null ? null
                            : callSite.invoke(context, executors, evaluatedBase, evaluatedName,
                                evaluateAll(context, arguments));
                    }
                };
            }
            case CALL -> {
                ELNode.Call call = (ELNode.Call) node;
                Evaluator target = compile(call.target());
                Evaluator[] arguments = compileAll(call.arguments());
                yield new Evaluator() {
                    @Override
                    @Nullable
                    Object evaluate(ELContext context) {
                        return invokeCallable(context, target.evaluate(context), evaluateAll(context, arguments));
                    }
                };
            }
            case UNARY -> compileUnary((ELNode.Unary) node);
            case BINARY -> compileBinary((ELNode.Binary) node);
            case TERNARY -> {
                ELNode.Ternary ternary = (ELNode.Ternary) node;
                Evaluator condition = compile(ternary.condition());
                Evaluator ifTrue = compile(ternary.ifTrue());
                Evaluator ifFalse = compile(ternary.ifFalse());
                yield new Evaluator() {
                    @Override
                    @Nullable
                    Object evaluate(ELContext context) {
                        return ELSupport.toBoolean(condition.evaluate(context)) ? ifTrue.evaluate(context) : ifFalse.evaluate(context);
                    }
                };
            }
            case ASSIGN -> {
                ELNode.Assign assign = (ELNode.Assign) node;
                yield new Evaluator() {
                    @Override
                    @Nullable
                    Object evaluate(ELContext context) {
                        return evaluateAssign(context, assign);
                    }
                };
            }
            case SEMICOLON -> {
                ELNode.Semicolon semicolon = (ELNode.Semicolon) node;
                Evaluator left = compile(semicolon.left());
                Evaluator right = compile(semicolon.right());
                yield new Evaluator() {
                    @Override
                    @Nullable
                    Object evaluate(ELContext context) {
                        return ELSupport.sequence(left.evaluate(context), right.evaluate(context));
                    }
                };
            }
            case LAMBDA -> {
                ELNode.Lambda lambda = (ELNode.Lambda) node;
                Evaluator body = compile(lambda.body());
                yield new Evaluator() {
                    @Override
                    Object evaluate(ELContext context) {
                        return ELLambdas.create(context, lambda.parameters(), body::evaluate);
                    }
                };
            }
            case SET_DATA -> {
                Evaluator[] elements = compileAll(((ELNode.SetData) node).elements());
                yield new Evaluator() {
                    @Override
                    Object evaluate(ELContext context) {
                        return ELCollections.set(evaluateAll(context, elements));
                    }
                };
            }
            case LIST_DATA -> {
                Evaluator[] elements = compileAll(((ELNode.ListData) node).elements());
                yield new Evaluator() {
                    @Override
                    Object evaluate(ELContext context) {
                        return ELCollections.list(evaluateAll(context, elements));
                    }
                };
            }
            case MAP_DATA -> {
                List<ELNode.MapData.MapEntry> entries = ((ELNode.MapData) node).entries();
                Evaluator[] keys = new Evaluator[entries.size()];
                Evaluator[] values = new Evaluator[entries.size()];
                for (int i = 0; i < entries.size(); i++) {
                    keys[i] = compile(entries.get(i).key());
                    ELNode value = entries.get(i).value();
                    values[i] = value == null ? new Constant(null) : compile(value);
                }
                yield new Evaluator() {
                    @Override
                    Object evaluate(ELContext context) {
                        Map<Object, Object> map = new LinkedHashMap<>();
                        for (int i = 0; i < keys.length; i++) {
                            map.put(keys[i].evaluate(context), values[i].evaluate(context));
                        }
                        return map;
                    }
                };
            }
        };
    }

    private Evaluator compileUnary(ELNode.Unary unary) {
        Evaluator operand = compile(unary.operand());
        return switch (unary.operator()) {
            case NEGATE -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELArithmetic.negate(operand.evaluate(context));
                }
            };
            case NOT -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELSupport.not(operand.evaluate(context));
                }
            };
            case EMPTY -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELSupport.isEmpty(operand.evaluate(context));
                }
            };
        };
    }

    /**
     * One class of evaluator per operator, so that the evaluation of the operands is a call site of its own.
     */
    @SuppressWarnings("java:S1541")
    private Evaluator compileBinary(ELNode.Binary binary) {
        Evaluator left = compile(binary.left());
        Evaluator right = compile(binary.right());
        return switch (binary.operator()) {
            case AND -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELSupport.toBoolean(left.evaluate(context)) && ELSupport.toBoolean(right.evaluate(context));
                }
            };
            case OR -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELSupport.toBoolean(left.evaluate(context)) || ELSupport.toBoolean(right.evaluate(context));
                }
            };
            case ADD -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELArithmetic.add(left.evaluate(context), right.evaluate(context));
                }
            };
            case SUBTRACT -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELArithmetic.subtract(left.evaluate(context), right.evaluate(context));
                }
            };
            case MULTIPLY -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELArithmetic.multiply(left.evaluate(context), right.evaluate(context));
                }
            };
            case DIVIDE -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELArithmetic.divide(left.evaluate(context), right.evaluate(context));
                }
            };
            case MODULO -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELArithmetic.mod(left.evaluate(context), right.evaluate(context));
                }
            };
            case CONCAT -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELArithmetic.concat(left.evaluate(context), right.evaluate(context));
                }
            };
            case LESS_THAN -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    Object leftValue = left.evaluate(context);
                    return leftValue != null && ELSupport.lessThan(leftValue, right.evaluate(context));
                }
            };
            case GREATER_THAN -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    Object leftValue = left.evaluate(context);
                    return leftValue != null && ELSupport.greaterThan(leftValue, right.evaluate(context));
                }
            };
            case LESS_THAN_OR_EQUAL -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELSupport.lessThanOrEqual(left.evaluate(context), right.evaluate(context));
                }
            };
            case GREATER_THAN_OR_EQUAL -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELSupport.greaterThanOrEqual(left.evaluate(context), right.evaluate(context));
                }
            };
            case EQUAL -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELSupport.equals(left.evaluate(context), right.evaluate(context));
                }
            };
            case NOT_EQUAL -> new Evaluator() {
                @Override
                Object evaluate(ELContext context) {
                    return ELSupport.notEquals(left.evaluate(context), right.evaluate(context));
                }
            };
        };
    }

    private Evaluator[] compileAll(List<ELNode> nodes) {
        Evaluator[] evaluators = new Evaluator[nodes.size()];
        for (int i = 0; i < evaluators.length; i++) {
            evaluators[i] = compile(nodes.get(i));
        }
        return evaluators;
    }

    private static Object[] evaluateAll(ELContext context, Evaluator[] evaluators) {
        if (evaluators.length == 0) {
            return NO_ARGUMENTS;
        }
        Object[] values = new Object[evaluators.length];
        for (int i = 0; i < evaluators.length; i++) {
            values[i] = evaluators[i].evaluate(context);
        }
        return values;
    }

    /**
     * Creates an interpreter for a parsed expression, binding its functions against the given context.
     *
     * @param context The context the expression is created for, can be {@code null}
     * @param node    The parsed expression
     * @return The interpreter
     */
    static ELInterpreter of(@Nullable ELContext context, ELNode node) {
        return of(context, node, loadExecutors());
    }

    static ELInterpreter of(@Nullable ELContext context, ELNode node, List<ELMethodExecutor> executors) {
        return new ELInterpreter(executors, bindFunctions(context, node, executors));
    }

    static ELInterpreter of(List<ELMethodExecutor> executors, Map<String, ELMethod> functions) {
        return new ELInterpreter(executors, functions);
    }

    static ELInterpreter of(Map<String, ELMethod> functions) {
        return of(loadExecutors(), functions);
    }

    List<ELMethodExecutor> executors() {
        return executors;
    }

    static List<ELMethodExecutor> orderExecutors(List<ELMethodExecutor> executors) {
        return executors.stream()
            .sorted(Comparator.comparingInt(ELMethodExecutor::getOrder))
            .toList();
    }

    /**
     * Creates an interpreter for a parsed expression holding no function, whose compiled evaluators are
     * therefore the same for every context and shared through the cache of the parser.
     *
     * @param root The evaluators compiled from the expression
     * @return The interpreter
     */
    static ELInterpreter sharing(Evaluator root) {
        return sharing(root, loadExecutors());
    }

    static ELInterpreter sharing(Evaluator root, List<ELMethodExecutor> executors) {
        ELInterpreter interpreter = new ELInterpreter(executors, Map.of());
        interpreter.root = root;
        return interpreter;
    }

    /**
     * @param node The parsed expression
     * @return Whether the expression invokes a function, which is bound against the context the expression is
     * created for
     */
    static boolean containsFunction(ELNode node) {
        if (node instanceof ELNode.Function) {
            return true;
        }
        for (ELNode child : children(node)) {
            if (containsFunction(child)) {
                return true;
            }
        }
        return false;
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
    static Map<String, ELMethod> bindFunctions(@Nullable ELContext context, ELNode node) {
        return bindFunctions(context, node, loadExecutors());
    }

    static Map<String, ELMethod> bindFunctions(@Nullable ELContext context,
                                               ELNode node,
                                               List<ELMethodExecutor> executors) {
        if (!containsFunction(node)) {
            return Map.of();
        }
        Map<String, ELMethod> bindings = new LinkedHashMap<>();
        bindFunctions(context, executors, node, bindings);
        return Map.copyOf(bindings);
    }

    private static void bindFunctions(@Nullable ELContext context,
                                      List<ELMethodExecutor> executors,
                                      ELNode node,
                                      Map<String, ELMethod> bindings) {
        if (node instanceof ELNode.Function function) {
            ELMethod method = context == null ? null : resolveFunction(context, executors,
                function.prefix(), function.localName());
            if (method != null) {
                int count = function.invocations().get(0).size();
                int parameters = method.getArguments().length;
                if (method.isVarArgs() ? count < parameters - 1 : count != parameters) {
                    throw new ELException("The function '" + qualifiedName(function.prefix(), function.localName())
                        + "' expects " + (method.isVarArgs() ? "at least " + (parameters - 1) : parameters)
                        + " argument(s) but " + count + " were provided");
                }
                bindings.put(qualifiedName(function.prefix(), function.localName()), method);
            } else if (!function.prefix().isEmpty()) {
                throw new ELException("Cannot resolve the function '"
                    + qualifiedName(function.prefix(), function.localName()) + "'");
            }
        }
        for (ELNode child : children(node)) {
            bindFunctions(context, executors, child, bindings);
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
        // a switch on the kind is a table switch; a pattern switch over the sealed types dispatches through a
        // method handle that classifies the node by scanning the case labels, which dominated the evaluation
        return switch (node.kind()) {
            case COMPOSITE -> evaluateComposite(context, (ELNode.Composite) node);
            case LITERAL_TEXT -> ((ELNode.LiteralText) node).text();
            case EVAL -> evaluate(context, ((ELNode.Eval) node).expression());
            case NULL_LITERAL -> null;
            case BOOLEAN_LITERAL -> ((ELNode.BooleanLiteral) node).value();
            case INTEGER_LITERAL -> ((ELNode.IntegerLiteral) node).value();
            case FLOATING_POINT_LITERAL -> ((ELNode.FloatingPointLiteral) node).value();
            case STRING_LITERAL -> ((ELNode.StringLiteral) node).value();
            case IDENTIFIER -> ELSandboxGuard.resolveIdentifier(context, ((ELNode.Identifier) node).name());
            case FUNCTION -> evaluateFunction(context, (ELNode.Function) node);
            case PROPERTY -> {
                ELNode.Property property = (ELNode.Property) node;
                Object base = evaluate(context, property.base());
                yield base == null ? null
                    : ELSandboxGuard.getValue(context, base, evaluate(context, property.property()));
            }
            case METHOD -> {
                ELNode.Method method = (ELNode.Method) node;
                Object base = evaluate(context, method.base());
                if (base == null) {
                    yield null;
                }
                Object property = evaluate(context, method.property());
                yield property == null ? null : invokeWithParams(
                    context, base, property, evaluateAll(context, method.arguments()));
            }
            case CALL -> {
                ELNode.Call call = (ELNode.Call) node;
                yield invokeCallable(context, evaluate(context, call.target()), evaluateAll(context, call.arguments()));
            }
            case UNARY -> evaluateUnary(context, (ELNode.Unary) node);
            case BINARY -> evaluateBinary(context, (ELNode.Binary) node);
            case TERNARY -> {
                ELNode.Ternary ternary = (ELNode.Ternary) node;
                yield ELSupport.toBoolean(evaluate(context, ternary.condition()))
                    ? evaluate(context, ternary.ifTrue())
                    : evaluate(context, ternary.ifFalse());
            }
            case ASSIGN -> evaluateAssign(context, (ELNode.Assign) node);
            case SEMICOLON -> {
                ELNode.Semicolon semicolon = (ELNode.Semicolon) node;
                yield ELSupport.sequence(evaluate(context, semicolon.left()), evaluate(context, semicolon.right()));
            }
            case LAMBDA -> {
                ELNode.Lambda lambda = (ELNode.Lambda) node;
                yield ELLambdas.create(context, lambda.parameters(), lambdaContext -> evaluate(lambdaContext, lambda.body()));
            }
            case SET_DATA -> ELCollections.set(evaluateAll(context, ((ELNode.SetData) node).elements()));
            case LIST_DATA -> ELCollections.list(evaluateAll(context, ((ELNode.ListData) node).elements()));
            case MAP_DATA -> evaluateMap(context, (ELNode.MapData) node);
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
            case ELNode.Property property -> {
                Object base = evaluate(context, property.base());
                if (base == null) {
                    throw new PropertyNotFoundException("Cannot resolve an lvalue with a null base object");
                }
                yield new Target(base, evaluate(context, property.property()));
            }
            // a semicolon expression is not an lvalue: the compiled path does not treat it as one, and
            // neither reference implementation does, so resolving one would evaluate its left operand for
            // nothing on every getType, isReadOnly and getValueReference
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
        if (target == null) {
            return null;
        }
        // the reference names the base and the property of an lvalue, so it is the same access as getType and
        // isReadOnly and the sandbox has the same say over it
        ELSandboxGuard.check(context, target.base(), target.property());
        return new ValueReference(target.base(), target.property());
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
        if ((operator == BinaryOperator.LESS_THAN || operator == BinaryOperator.GREATER_THAN) && left == null) {
            return false;
        }
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
        return ELSandboxGuard.assignProperty(context, target.base(), target.property(), value);
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
            result = invokeCallable(context, result, evaluateAll(context, invocations.get(i)));
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
            return invokeCallable(context, identifier, evaluateAll(context, firstArguments));
        }
        ELMethod method = resolveMappedFunction(function);
        if (method != null) {
            return method.invoke(context, null, evaluateAll(context, firstArguments));
        }
        if (prefix.isEmpty()) {
            ImportHandler importHandler = context.getImportHandler();
            if (importHandler != null) {
                Class<?> resolvedClass = importHandler.resolveClass(localName);
                if (resolvedClass != null) {
                    return invokeCallable(context, new ELClass(resolvedClass), evaluateAll(context, firstArguments));
                }
                Class<?> staticClass = importHandler.resolveStatic(localName);
                if (staticClass != null) {
                    return invokeWithParams(context, new ELClass(staticClass), localName,
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
    private ELMethod resolveMappedFunction(ELNode.Function function) {
        return functions.get(qualifiedName(function.prefix(), function.localName()));
    }

    @Nullable
    static Object invokeWithParams(ELContext context,
                                   List<ELMethodExecutor> executors,
                                   @Nullable Object base,
                                   @Nullable Object method,
                                   Class<?> @Nullable [] paramTypes,
                                   Object @Nullable [] arguments) {
        if (base == null || method == null) {
            throw new PropertyNotFoundException("Cannot resolve a method on a null base object");
        }
        ELSandboxGuard.check(context, base, method);
        ELMethod resolved = resolveMethod(context, executors, base, method, paramTypes, arguments);
        return resolved.invoke(context, base, arguments);
    }

    @Nullable
    private Object invokeWithParams(ELContext context,
                                    @Nullable Object base,
                                    @Nullable Object method,
                                    Object @Nullable [] arguments) {
        return invokeWithParams(context, executors, base, method, null, arguments);
    }

    @Nullable
    private Object invokeCallable(ELContext context, @Nullable Object target, Object... arguments) {
        ELSandboxGuard.check(context, target, null);
        if (target instanceof LambdaExpression lambda) {
            lambda.setELContext(context);
            return lambda.invoke(context, arguments);
        }
        if (target instanceof ELClass) {
            return invokeWithParams(context, target, "<init>", arguments);
        }
        return ELResolution.invokeCallable(context, target, arguments);
    }

    static ELMethod resolveMethod(ELContext context,
                                  List<ELMethodExecutor> executors,
                                  @Nullable Object base,
                                  @Nullable Object method,
                                  Class<?> @Nullable [] paramTypes,
                                  Object @Nullable [] arguments) {
        if (base == null || method == null) {
            throw new PropertyNotFoundException("Cannot resolve a method on a null base object");
        }
        for (ELMethodExecutor executor : executors) {
            ELMethod resolved = executor.resolve(context, base, method, ELArguments.of(paramTypes), arguments);
            if (resolved != null) {
                return resolved;
            }
        }
        // the executors that were consulted are what says which of the remedies applies: the reflective one is
        // in the list only when the optional module is on the classpath
        throw ELMethodDiagnostics.notFound(context, base, method, arguments, executors);
    }

    @Nullable
    private static ELMethod resolveFunction(ELContext context,
                                            List<ELMethodExecutor> executors,
                                            String prefix,
                                            String localName) {
        for (ELMethodExecutor executor : executors) {
            ELMethod resolved = executor.resolveFunction(context, prefix, localName);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    private static List<ELMethodExecutor> loadExecutors() {
        return SoftServiceLoader.load(ELMethodExecutor.class, contextClassLoader()).collectAll();
    }

    private static ClassLoader contextClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader == null ? ELInterpreter.class.getClassLoader() : classLoader;
    }

    @Nullable
    private Object resolveIdentifierOrNull(ELContext context, String name) {
        if (context.isLambdaArgument(name)) {
            return context.getLambdaArgument(name);
        }
        if (context.getVariableMapper() != null) {
            var expression = context.getVariableMapper().resolveVariable(name);
            if (expression != null) {
                return expression.getValue(context);
            }
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

    /**
     * An evaluator compiled from a node: the node is classified once, and the evaluation of its children is a
     * virtual call from a call site specific to the kind of the parent, which the JIT compiler inlines, where
     * a walk of the tree classifies every node on every evaluation from a single call site.
     */
    abstract static class Evaluator {

        @Nullable
        abstract Object evaluate(ELContext context);
    }

    private static final class Constant extends Evaluator {
        @Nullable
        private final Object value;

        Constant(@Nullable Object value) {
            this.value = value;
        }

        @Override
        @Nullable
        Object evaluate(ELContext context) {
            return value;
        }
    }

    /**
     * The method a call of the expression resolved to, kept in the compiled evaluator of that call.
     *
     * <p>Resolving a method means selecting an overload among the candidates of its name, which is work that
     * only depends on the type of the base object and on the name. A call site therefore remembers the method
     * it resolved for the type it last saw, and evaluates straight into it while the type does not change.
     * Only a method that {@link ELMethod#isReusable() states it can be invoked again} is kept: one selected
     * from the runtime types of the arguments is resolved anew on every evaluation, because other arguments
     * could select another overload.</p>
     *
     * <p>The cache is a single field read without synchronization: a race recomputes the same answer, and the
     * sandbox is consulted on every evaluation, before the cache is.</p>
     */
    static final class MethodCallSite {

        @Nullable
        private volatile Resolved resolved;

        @Nullable
        Object invoke(ELContext context,
                      List<ELMethodExecutor> executors,
                      Object base,
                      Object method,
                      Object @Nullable [] arguments) {
            ELSandboxGuard.check(context, base, method);
            Class<?> type = base instanceof ELClass elClass ? elClass.getKlass() : base.getClass();
            Resolved cached = resolved;
            if (cached != null && cached.type() == type && cached.name().equals(method)) {
                return cached.method().invoke(context, base, arguments);
            }
            ELMethod found = resolveMethod(context, executors, base, method, null, arguments);
            if (found.isReusable()) {
                resolved = new Resolved(type, method, found);
            }
            return found.invoke(context, base, arguments);
        }

        private record Resolved(Class<?> type, Object name, ELMethod method) {
        }
    }
}
