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
package io.micronaut.el.processor.compiler;

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;
import io.micronaut.el.parser.ast.BinaryOperator;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.el.processor.visitor.ELTypes;
import io.micronaut.el.runtime.ELArithmetic;
import io.micronaut.el.runtime.ELCollections;
import io.micronaut.el.runtime.ELLambdaBody;
import io.micronaut.el.runtime.ELLambdas;
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.el.runtime.ELSupport;
import io.micronaut.el.stream.ELStream;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.GenericPlaceholderElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.PrimitiveElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.ast.WildcardElement;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;
import jakarta.el.ELClass;
import jakarta.el.LambdaExpression;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The compiler lowering the abstract syntax tree of an expression to the source model of Micronaut
 * SourceGen.
 *
 * <p>Every construct of the language is compiled to the equivalent Java expression. The property
 * accesses, the method invocations, the static references and the functions whose types are known at
 * compilation time are compiled to direct invocations, the remaining ones are compiled to the resolution
 * described in the sections 1.5 and 1.6 of the specification. The operators whose operand types are known
 * at compilation time are compiled to the Java operators the runtime would reach, see
 * {@link #inlineBinary(BinaryOperator, Typed, Typed)}.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELCompiler {

    private static final ClassTypeDef EL_SUPPORT = ClassTypeDef.of(ELSupport.class);
    private static final ClassTypeDef EL_ARITHMETIC = ClassTypeDef.of(ELArithmetic.class);
    private static final ClassTypeDef EL_RESOLUTION = ClassTypeDef.of(ELResolution.class);
    private static final ClassTypeDef EL_LAMBDAS = ClassTypeDef.of(ELLambdas.class);
    private static final ClassTypeDef EL_COLLECTIONS = ClassTypeDef.of(ELCollections.class);
    private static final ClassTypeDef EL_CLASS = ClassTypeDef.of(ELClass.class);
    private static final ClassTypeDef EL_STREAM = ClassTypeDef.of(ELStream.class);
    private static final String STREAM = "stream";
    private static final TypeDef LAMBDA_EXPRESSION = TypeDef.of(LambdaExpression.class);
    private static final TypeDef STRING = TypeDef.of(String.class);
    private static final TypeDef BOOLEAN = TypeDef.Primitive.BOOLEAN;
    /**
     * The methods of {@link Object} an interface may redeclare abstract, which do not make it functional.
     */
    private static final Set<String> OBJECT_METHODS = Set.of("equals/1", "hashCode/0", "toString/0");
    private static final int EXACT = 0;
    private static final int WIDENING = 1;
    private static final int COERCIBLE = 3;

    private final CompilationContext context;
    /**
     * The identifiers resolved once into a local for the evaluation being compiled, by name.
     */
    private final Map<String, Typed> shared = new LinkedHashMap<>();
    private final java.util.Set<String> unresolvedIdentifiers = new java.util.LinkedHashSet<>();
    private int lambdas;
    @Nullable
    private ClassElement evaluationType;
    @Nullable
    private ExpressionDef evaluationExpression;

    /**
     * @param context The compilation context
     */
    public ELCompiler(CompilationContext context) {
        this.context = context;
    }

    /**
     * Compiles an expression.
     *
     * @param node      The parsed expression
     * @param elContext The expression holding the {@code jakarta.el.ELContext}
     * @return The compiled expression
     */
    public ExpressionDef compile(ELNode node, ExpressionDef elContext) {
        return compileTyped(node, elContext).expression();
    }

    /**
     * Compiles the evaluation of an expression as the body of a method returning its result.
     *
     * <p>An identifier the expression resolves more than once is resolved once, into a local the references
     * share, when nothing in the expression can change what the identifier denotes during the evaluation: the
     * section 1.5.1 of the specification resolves an identifier through the lambda arguments, the variable
     * mapper and the resolvers, and only the assignment and the semicolon operators can alter any of these
     * within one evaluation. The bodies of the lambda expressions keep resolving through their own context.</p>
     *
     * <p>When the expression holds a lambda expression, the context is first bound to a local: a Java lambda
     * compiled from it captures the context, and the Java source writer of SourceGen 2.1.0 cannot render a
     * captured method parameter, while its bytecode writer captures a local.</p>
     *
     * @param node      The parsed expression
     * @param elContext The expression holding the {@code jakarta.el.ELContext}
     * @param body      The compilation of the result with the context to use, once the shared identifiers are in
     *                  place
     * @return The body of the method
     */
    public StatementDef compileEvaluation(ELNode node, ExpressionDef elContext, Function<ExpressionDef, Typed> body) {
        List<String> shared = new ArrayList<>();
        if (!hasSideEffects(node)) {
            Map<String, Integer> references = new LinkedHashMap<>();
            Map<String, Integer> lambdaReferences = new LinkedHashMap<>();
            countIdentifiers(node, references, lambdaReferences, false, Set.of());
            lambdaReferences.forEach((name, count) -> references.merge(name, count, Integer::sum));
            references.forEach((name, count) -> {
                // a declared variable a lambda passed to a method refers to is resolved once, outside the
                // lambda: the lambda runs within the evaluation, for every element of a stream
                boolean inLambda = lambdaReferences.containsKey(name) && context.variableType(name) != null;
                if ((count > 1 || inLambda) && context.resolveClass(name) == null && context.resolveStaticField(name) == null) {
                    shared.add(name);
                }
            });
        }
        evaluationType = null;
        unresolvedIdentifiers.clear();
        if (containsLambda(node)) {
            return elContext.newLocal("elContext", local -> share(shared, 0, local, body));
        }
        return share(shared, 0, elContext, body);
    }

    /**
     * @return The static type of the result of the last {@link #compileEvaluation}, or {@code null} when it is
     * not known
     */
    @Nullable
    public ClassElement evaluationType() {
        return evaluationType;
    }

    /**
     * The expected type inferred for an expression whose declaration omits it: the static type of the result,
     * a primitive as the wrapper the evaluation returns, {@link Object} when the compiler does not know the
     * type.
     *
     * @return The inferred type
     */
    public ClassElement inferredEvaluationType() {
        return inferred(evaluationType);
    }

    /**
     * The static type of an expression, for the return type of a method expression whose declaration omits it.
     *
     * @param node The parsed expression
     * @return The inferred type
     */
    public ClassElement inferredType(ELNode node) {
        unresolvedIdentifiers.clear();
        return inferred(compileTyped(node, ExpressionDef.nullValue()).type());
    }

    private static ClassElement inferred(@Nullable ClassElement type) {
        if (type == null || isUnknown(type)) {
            return ClassElement.of(Object.class);
        }
        if (type.isPrimitive() && !type.getName().equals("void")) {
            return ClassElement.of(wrapperClass(type.getName()));
        }
        return type;
    }

    private static Class<?> wrapperClass(String primitive) {
        return switch (primitive) {
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            case "char" -> Character.class;
            default -> Object.class;
        };
    }

    /**
     * @param expected The expected type of a value expression
     * @return Whether the result of the last {@link #compileEvaluation} is known to be of the expected type, so
     * that the coercion of the section 1.23 of the specification is the identity
     */
    public boolean evaluatesTo(ClassElement expected) {
        ClassElement type = evaluationType;
        if (expected.getName().equals(Object.class.getName())) {
            return true;
        }
        if (type == null) {
            return false;
        }
        if (type.isPrimitive()) {
            return expected.getName().equals(type.getName()) || expected.getName().equals(wrapper(type.getName()));
        }
        if (expected.isPrimitive()) {
            return false;
        }
        // a null String coerces to the empty string, which only a value known not to be null can skip
        if (expected.getName().equals(String.class.getName()) && !isNonNull(evaluationExpression)) {
            return false;
        }
        return type.getName().equals(expected.getName()) || type.isAssignable(expected.getName());
    }

    /**
     * @return Whether an expression is known not to evaluate to null: a literal, a concatenation, a conditional
     * of such expressions
     */
    private static boolean isNonNull(@Nullable ExpressionDef expression) {
        return switch (expression) {
            case ExpressionDef.Constant constant -> constant.value() != null;
            case ExpressionDef.StringConcatenation ignored -> true;
            case ExpressionDef.IfElse conditional -> isNonNull(conditional.ifExpression()) && isNonNull(conditional.elseExpression());
            case null, default -> false;
        };
    }

    private static String wrapper(String primitive) {
        return switch (primitive) {
            case "boolean" -> "java.lang.Boolean";
            case "byte" -> "java.lang.Byte";
            case "short" -> "java.lang.Short";
            case "int" -> "java.lang.Integer";
            case "long" -> "java.lang.Long";
            case "float" -> "java.lang.Float";
            case "double" -> "java.lang.Double";
            case "char" -> "java.lang.Character";
            default -> primitive;
        };
    }

    private StatementDef share(List<String> names, int index, ExpressionDef ctx, Function<ExpressionDef, Typed> body) {
        if (index == names.size()) {
            Typed result = body.apply(ctx);
            evaluationType = result.type();
            evaluationExpression = result.expression();
            return result.expression().returning();
        }
        String name = names.get(index);
        Typed resolved = resolveIdentifier(name, ctx);
        // the name of the local cannot hold a `$`: the Java source writer reads it as a format placeholder
        return resolved.expression().newLocal("shared" + index, local -> {
            shared.put(name, new Typed(local, resolved.type()));
            try {
                return share(names, index + 1, ctx, body);
            } finally {
                shared.remove(name);
            }
        });
    }

    private static boolean containsLambda(ELNode node) {
        return node instanceof ELNode.Lambda || children(node).stream().anyMatch(ELCompiler::containsLambda);
    }

    private static boolean hasSideEffects(ELNode node) {
        return switch (node) {
            case ELNode.Assign ignored -> true;
            case ELNode.Semicolon ignored -> true;
            default -> children(node).stream().anyMatch(ELCompiler::hasSideEffects);
        };
    }

    /**
     * Counts the references to the identifiers, outside the lambda expressions and, separately, inside the
     * lambda expressions passed to a method, which are compiled to Java lambdas running within the evaluation.
     * The bodies of the other lambda expressions, values invoked later, are not counted.
     */
    private static void countIdentifiers(ELNode node, Map<String, Integer> into, Map<String, Integer> lambdas, boolean inLambda, Set<String> parameters) {
        if (node instanceof ELNode.Identifier identifier) {
            if (!parameters.contains(identifier.name())) {
                (inLambda ? lambdas : into).merge(identifier.name(), 1, Integer::sum);
            }
        } else if (node instanceof ELNode.Lambda lambda) {
            if (inLambda) {
                countIdentifiers(lambda.body(), into, lambdas, true, bound(parameters, lambda));
            }
        } else if (node instanceof ELNode.Method method) {
            countIdentifiers(method.base(), into, lambdas, inLambda, parameters);
            countIdentifiers(method.property(), into, lambdas, inLambda, parameters);
            for (ELNode argument : method.arguments()) {
                if (argument instanceof ELNode.Lambda lambda) {
                    countIdentifiers(lambda.body(), into, lambdas, true, bound(parameters, lambda));
                } else {
                    countIdentifiers(argument, into, lambdas, inLambda, parameters);
                }
            }
        } else {
            children(node).forEach(child -> countIdentifiers(child, into, lambdas, inLambda, parameters));
        }
    }

    private static Set<String> bound(Set<String> parameters, ELNode.Lambda lambda) {
        Set<String> all = new java.util.HashSet<>(parameters);
        all.addAll(lambda.parameters());
        return all;
    }

    private static List<ELNode> children(ELNode node) {
        return switch (node) {
            case ELNode.Composite composite -> composite.parts();
            case ELNode.Eval eval -> List.of(eval.expression());
            case ELNode.Property property -> List.of(property.base(), property.property());
            case ELNode.Method method -> concat(List.of(method.base(), method.property()), method.arguments());
            case ELNode.Call call -> concat(List.of(call.target()), call.arguments());
            case ELNode.Function function -> function.invocations().stream().flatMap(List::stream).toList();
            case ELNode.Unary unary -> List.of(unary.operand());
            case ELNode.Binary binary -> List.of(binary.left(), binary.right());
            case ELNode.Ternary ternary -> List.of(ternary.condition(), ternary.ifTrue(), ternary.ifFalse());
            case ELNode.Assign assign -> List.of(assign.target(), assign.value());
            case ELNode.Semicolon semicolon -> List.of(semicolon.left(), semicolon.right());
            case ELNode.Lambda lambda -> List.of(lambda.body());
            case ELNode.SetData set -> set.elements();
            case ELNode.ListData list -> list.elements();
            case ELNode.MapData map -> map.entries().stream()
                .flatMap(entry -> entry.value() == null ? Stream.of(entry.key()) : Stream.of(entry.key(), entry.value()))
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
     * Compiles the base object and the property of an lvalue, as described in the section 1.13 of the
     * specification.
     *
     * @param node      The parsed expression
     * @param elContext The expression holding the {@code jakarta.el.ELContext}
     * @return The lvalue or {@code null} when the expression is not an lvalue
     */
    @Nullable
    public LValue compileLValue(ELNode node, ExpressionDef elContext) {
        ELNode expression = node instanceof ELNode.Eval eval ? eval.expression() : node;
        if (expression instanceof ELNode.Identifier identifier) {
            return new LValue(null, ExpressionDef.constant(identifier.name()));
        }
        if (expression instanceof ELNode.Property property) {
            return new LValue(
                compile(property.base(), elContext),
                compile(property.property(), elContext)
            );
        }
        return null;
    }

    /**
     * Invokes a static method of the runtime with the signature the runtime actually declares.
     *
     * <p>The convenience overload of {@code invokeStatic} infers the parameter types from the arguments, which
     * produces a descriptor that does not exist: the runtime declares its dynamic parameters as {@link Object},
     * and an argument whose static type is known would narrow them. The Java source writer hides this because
     * the compiler resolves the overload, the bytecode writer does not. A variable arity method is invoked with
     * its trailing arguments packed into an array, for the same reason.</p>
     *
     * @param owner     The declaring type
     * @param name      The method name
     * @param returning The type the result is used as, which a generic method reaches through a cast
     * @param values    The arguments
     * @return The invocation
     */
    private ExpressionDef runtime(ClassTypeDef owner, String name, TypeDef returning, ExpressionDef... values) {
        return runtime(owner, name, returning, List.of(values));
    }

    /**
     * Invokes a static method of the runtime with the signature the runtime actually declares, for the writers.
     *
     * @param owner     The declaring type
     * @param name      The method name
     * @param returning The type the result is used as
     * @param values    The arguments
     * @return The invocation
     * @see #runtime(ClassTypeDef, String, TypeDef, ExpressionDef...)
     */
    public ExpressionDef invokeRuntime(ClassTypeDef owner, String name, TypeDef returning, ExpressionDef... values) {
        return runtime(owner, name, returning, List.of(values));
    }

    /**
     * @param owner     The declaring type
     * @param name      The method name
     * @param returning The return type
     * @param values    The arguments
     * @return The invocation
     * @see #runtime(ClassTypeDef, String, TypeDef, ExpressionDef...)
     */
    private ExpressionDef runtime(ClassTypeDef owner,
                                  String name,
                                  TypeDef returning,
                                  List<? extends ExpressionDef> values) {
        Method method = runtimeMethod(owner, name, values.size());
        if (method == null) {
            return owner.invokeStatic(name, returning, values);
        }
        Class<?>[] parameters = method.getParameterTypes();
        List<TypeDef> parameterTypes = new ArrayList<>(parameters.length);
        for (Class<?> parameter : parameters) {
            parameterTypes.add(TypeDef.of(parameter));
        }
        List<ExpressionDef> arguments = new ArrayList<>(parameters.length);
        boolean variadic = method.isVarArgs()
            && (values.size() != parameters.length || !(values.get(values.size() - 1).type() instanceof TypeDef.Array));
        int fixed = variadic ? parameters.length - 1 : parameters.length;
        for (int i = 0; i < fixed; i++) {
            arguments.add(values.get(i));
        }
        if (variadic) {
            Class<?> componentType = parameters[parameters.length - 1].getComponentType();
            arguments.add(TypeDef.of(componentType).array().instantiate(values.subList(fixed, values.size())));
        }
        // a generic method erases to its bound, so the descriptor uses the declared return type and the result
        // is cast to the type the caller asked for
        TypeDef declaredReturn = TypeDef.of(method.getReturnType());
        ExpressionDef invocation = owner.invokeStatic(name, parameterTypes, declaredReturn, arguments);
        return declaredReturn.equals(returning) ? invocation : invocation.cast(returning);
    }

    /**
     * The runtime method of the given name taking the arguments. The runtime is on the classpath of the
     * processor, so its signature is read reflectively: the language front ends do not agree on the variable
     * arity of a method loaded from the classpath, Groovy reports it as false and KSP describes the parameter
     * by its component type, and the runtime declares no overloads.
     */
    @Nullable
    private static Method runtimeMethod(ClassTypeDef owner, String name, int argumentCount) {
        Class<?> runtime = ClassUtils.forName(owner.getName(), ELCompiler.class.getClassLoader()).orElse(null);
        if (runtime == null) {
            return null;
        }
        for (Method method : runtime.getMethods()) {
            if (!method.getName().equals(name) || !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            int parameters = method.getParameterCount();
            if (argumentCount == parameters || (method.isVarArgs() && argumentCount >= parameters - 1)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Compiles an expression, with its static type.
     *
     * @param node The parsed expression
     * @param ctx  The expression holding the {@code jakarta.el.ELContext}
     * @return The compiled expression and its type
     */
    public Typed compileTyped(ELNode node, ExpressionDef ctx) {
        return switch (node) {
            case ELNode.Eval eval -> compileTyped(eval.expression(), ctx);
            case ELNode.Composite composite -> compileComposite(composite, ctx);
            case ELNode.LiteralText literal -> new Typed(ExpressionDef.constant(literal.text()), ClassElement.of(String.class));
            case ELNode.NullLiteral ignored -> dynamic(ExpressionDef.nullValue());
            case ELNode.BooleanLiteral literal -> new Typed(ExpressionDef.constant(Boolean.valueOf(literal.value())), ClassElement.of(Boolean.class));
            case ELNode.IntegerLiteral literal -> new Typed(integerLiteral(literal.image()), ClassElement.of(integerLiteralType(literal.image())));
            case ELNode.FloatingPointLiteral literal -> new Typed(floatingPointLiteral(literal.image()), ClassElement.of(floatingPointLiteralType(literal.image())));
            case ELNode.StringLiteral literal -> new Typed(ExpressionDef.constant(literal.value()), ClassElement.of(String.class));
            case ELNode.Identifier identifier -> compileIdentifier(identifier, ctx);
            case ELNode.Property property -> compileProperty(property, ctx);
            case ELNode.Method method -> compileMethod(method, ctx);
            case ELNode.Function function -> compileFunction(function, ctx);
            case ELNode.Call call -> dynamic(runtime(EL_RESOLUTION, "invokeCallable", TypeDef.OBJECT,
                arguments(ctx, compile(call.target(), ctx), call.arguments())));
            case ELNode.Unary unary -> compileUnary(unary, ctx);
            case ELNode.Binary binary -> compileBinary(binary, ctx);
            case ELNode.Ternary ternary -> compileTernary(ternary, ctx);
            case ELNode.Assign assign -> dynamic(compileAssign(assign, ctx));
            case ELNode.Semicolon semicolon -> dynamic(runtime(EL_SUPPORT, "sequence", TypeDef.OBJECT,
                compile(semicolon.left(), ctx), compile(semicolon.right(), ctx)));
            case ELNode.Lambda lambda -> new Typed(compileLambda(lambda, ctx), elementOf(LambdaExpression.class));
            case ELNode.ListData list -> dynamic(runtime(EL_COLLECTIONS, "list", TypeDef.OBJECT,
                compileAll(list.elements(), ctx)));
            case ELNode.SetData set -> dynamic(runtime(EL_COLLECTIONS, "set", TypeDef.OBJECT,
                compileAll(set.elements(), ctx)));
            case ELNode.MapData map -> dynamic(runtime(EL_COLLECTIONS, "map", TypeDef.OBJECT,
                compileMapEntries(map, ctx)));
        };
    }

    /**
     * A composite expression is the concatenation of its parts coerced to strings, section 1.2.1 of the
     * specification, compiled to one Java string concatenation.
     */
    private Typed compileComposite(ELNode.Composite composite, ExpressionDef ctx) {
        List<ELNode> parts = composite.parts();
        if (parts.isEmpty()) {
            return new Typed(ExpressionDef.constant(""), ClassElement.of(String.class));
        }
        if (parts.size() == 1) {
            return compileTyped(parts.get(0), ctx);
        }
        ExpressionDef result = stringOperand(compileTyped(parts.get(0), ctx));
        for (ELNode part : parts.subList(1, parts.size())) {
            result = concat(result, stringOperand(compileTyped(part, ctx)));
        }
        return new Typed(result, ClassElement.of(String.class));
    }

    /**
     * Concatenates two strings, two literals into one.
     */
    private static ExpressionDef concat(ExpressionDef left, ExpressionDef right) {
        if (left instanceof ExpressionDef.Constant first && first.value() instanceof String a
            && right instanceof ExpressionDef.Constant second && second.value() instanceof String b) {
            return ExpressionDef.constant(a + b);
        }
        return left.stringConcat(right);
    }

    /**
     * Coerces an operand of a string concatenation to a string as described in the section 1.23.2 of the
     * specification, with what the compiler knows of its type: a primitive prints as its wrapper does, which
     * is what {@link String#valueOf} does; a reference that cannot be an enum is its {@code toString()}, or
     * empty when null; anything else, an enum, an interface or an unknown type, is coerced by the runtime.
     *
     * <p>A primitive is not handed to the concatenation as it is: the bytecode writer of SourceGen 2.1.0 boxes
     * the dynamic parts of a concatenation while declaring them primitive to the concat factory, which the
     * verifier rejects.</p>
     */
    private ExpressionDef stringOperand(Typed operand) {
        ExpressionDef expression = operand.expression();
        ClassElement type = operand.type();
        if (expression instanceof ExpressionDef.Constant constant && constant.value() instanceof String
            || expression instanceof ExpressionDef.StringConcatenation) {
            return expression;
        }
        if (type != null && type.isPrimitive() && expression.type() instanceof TypeDef.Primitive primitive) {
            // String.valueOf has no overload for the narrower integral primitives, they widen to int
            TypeDef.Primitive parameter = primitive.equals(TypeDef.Primitive.BYTE) || primitive.equals(TypeDef.Primitive.SHORT)
                ? TypeDef.Primitive.INT : primitive;
            return TypeDef.STRING.invokeStatic("valueOf", List.of(parameter), STRING,
                List.of(parameter.equals(primitive) ? expression : expression.cast(parameter)));
        }
        if (type != null && !type.isInterface() && !type.isEnum()
            && !type.getName().equals(Object.class.getName()) && !type.getName().equals(Enum.class.getName())) {
            return ClassTypeDef.of(Objects.class).invokeStatic("toString", List.of(TypeDef.OBJECT, STRING), STRING,
                List.of(expression, ExpressionDef.constant("")));
        }
        return runtime(EL_SUPPORT, "coerceToString", STRING, expression);
    }

    private Typed compileIdentifier(ELNode.Identifier identifier, ExpressionDef ctx) {
        String name = identifier.name();
        // a parameter of an enclosing lambda expression shadows a shared identifier
        Typed local = context.inValueLambda() || context.isLambdaParameter(name) ? null : shared.get(name);
        if (local != null) {
            return local;
        }
        return resolveIdentifier(name, ctx);
    }

    /**
     * What the compiler knows of an identifier selects the resolution: a parameter of an enclosing lambda
     * expression is the Java parameter holding it, a declared variable goes through the variable mapper and the
     * resolvers, and only an identifier the compiler knows nothing about takes every step of the section 1.5.1.
     */
    /**
     * @return The identifiers the compiled code resolves dynamically because nothing declares them: not a
     * variable, not a lambda parameter, not an import
     */
    public java.util.Set<String> unresolvedIdentifiers() {
        return unresolvedIdentifiers;
    }

    /**
     * @param node A parsed expression
     * @return Whether the expression assigns or sequences, which makes what an identifier denotes dynamic
     */
    public static boolean hasAssignments(ELNode node) {
        return hasSideEffects(node);
    }

    private Typed resolveIdentifier(String name, ExpressionDef ctx) {
        Typed lambdaParameter = context.lambdaParameter(name);
        if (lambdaParameter != null) {
            return lambdaParameter;
        }
        ClassElement variableType = context.variableType(name);
        if (variableType != null) {
            ExpressionDef variable = runtime(EL_RESOLUTION, "resolveVariable", TypeDef.OBJECT, ctx, ExpressionDef.constant(name));
            return new Typed(variable.cast(TypeDef.erasure(variableType)), variableType);
        }
        FieldElement staticField = context.resolveStaticField(name);
        if (staticField != null) {
            return staticFieldAccess(staticField);
        }
        unresolvedIdentifiers.add(name);
        return dynamic(runtime(EL_RESOLUTION, "resolveIdentifier", TypeDef.OBJECT, ctx, ExpressionDef.constant(name)));
    }

    private Typed compileProperty(ELNode.Property property, ExpressionDef ctx) {
        String propertyName = constantName(property.property());
        if (propertyName != null && property.base() instanceof ELNode.Identifier identifier) {
            ClassElement importedClass = context.resolveClass(identifier.name());
            if (importedClass != null) {
                FieldElement field = findStaticField(importedClass, propertyName);
                if (field != null) {
                    return staticFieldAccess(field);
                }
                throw new ELCompilationException("The class " + importedClass.getName()
                    + " does not declare the public static field '" + propertyName + "'");
            }
        }
        Typed base = compileTyped(property.base(), ctx);
        ClassElement baseType = base.type();
        if (baseType != null) {
            Typed collection = compileCollectionAccess(base, baseType, property.property(), ctx);
            if (collection != null) {
                return collection;
            }
        }
        if (propertyName != null && baseType != null) {
            MethodElement reader = findPropertyReader(baseType, propertyName);
            if (reader != null) {
                return new Typed(base.expression().invoke(reader), returnType(baseType, reader));
            }
            warnUnknownMember(baseType, "property", propertyName);
        }
        return dynamic(runtime(EL_RESOLUTION, "getValue", TypeDef.OBJECT,
            ctx, base.expression(), compile(property.property(), ctx)));
    }

    /**
     * Compiles the access to an element of a base known to be a {@link Map} or a {@link List} to the direct
     * access the {@code MapELResolver} and the {@code ListELResolver} of the specification perform: the value
     * of the key, or the element at the index coerced to an integer. The types of the elements come from the
     * type arguments of the base.
     *
     * @return The access, or {@code null} when the base is neither
     */
    @Nullable
    private Typed compileCollectionAccess(Typed base, ClassElement baseType, ELNode property, ExpressionDef ctx) {
        if (baseType.isAssignable(Map.class)) {
            ClassElement valueType = resolved(baseType.getTypeArguments(Map.class).get("V"));
            ExpressionDef value = runtime(EL_RESOLUTION, "mapValue", TypeDef.OBJECT, base.expression(), compile(property, ctx));
            return typed(value, valueType);
        }
        if (baseType.isAssignable(List.class)) {
            ClassElement elementType = resolved(baseType.getTypeArguments(List.class).get("E"));
            ExpressionDef element = runtime(EL_RESOLUTION, "listElement", TypeDef.OBJECT, base.expression(), compile(property, ctx));
            return typed(element, elementType);
        }
        return null;
    }

    /**
     * @return The value as the given type when it is known, cast to it, the value untyped otherwise
     */
    private static Typed typed(ExpressionDef value, @Nullable ClassElement type) {
        if (type == null || type.isPrimitive() || isUnknown(type)) {
            return dynamic(value);
        }
        return new Typed(value.cast(erasure(type)), type);
    }

    private Typed compileMethod(ELNode.Method method, ExpressionDef ctx) {
        String methodName = constantName(method.property());
        if (methodName != null && method.base() instanceof ELNode.Identifier identifier) {
            ClassElement importedClass = context.resolveClass(identifier.name());
            if (importedClass != null) {
                MethodElement staticMethod = selectMethod(importedClass, methodName, method.arguments(), true, ctx);
                if (staticMethod != null) {
                    return new Typed(
                        ClassTypeDef.of(importedClass).invokeStatic(staticMethod,
                            coercedArguments(staticMethod, method.arguments(), ctx)),
                        staticMethod.getReturnType()
                    );
                }
                // the class declares overloads of the method, the resolution is deferred to the resolvers
                return dynamic(runtime(EL_RESOLUTION, "invoke", TypeDef.OBJECT,
                    arguments(ctx, elClass(importedClass), compile(method.property(), ctx), method.arguments())));
            }
        }
        Typed base = compileTyped(method.base(), ctx);
        ClassElement baseType = base.type();
        if (STREAM.equals(methodName) && method.arguments().isEmpty() && baseType != null && isStreamable(baseType)) {
            // the section 2.3.1 of the specification defines stream() as the source of a pipeline
            return new Typed(runtime(EL_STREAM, "of", EL_STREAM, ctx, base.expression()), streamOf(baseType));
        }
        if (methodName != null && baseType != null) {
            MethodElement target = selectMethod(baseType, methodName, method.arguments(), false, ctx);
            if (target != null) {
                return new Typed(
                    base.expression().invoke(target, coercedArguments(baseType, target, method.arguments(), ctx)),
                    returnType(baseType, target)
                );
            }
            warnUnknownMember(baseType, "method", methodName + "(" + method.arguments().size() + " argument(s))");
        }
        return dynamic(runtime(EL_RESOLUTION, "invoke", TypeDef.OBJECT,
            arguments(ctx, base.expression(), compile(method.property(), ctx), method.arguments())));
    }

    /**
     * A member the static type of the base does not declare is almost always a mistake, so it is reported, as a
     * warning: a custom {@code jakarta.el.ELResolver} may well serve it at runtime, which is where the access is
     * then resolved. The types the standard resolvers read dynamically, by key, index or name, are not reported.
     */
    private void warnUnknownMember(ClassElement type, String kind, String member) {
        if (isResolvedDynamically(type)) {
            return;
        }
        context.getVisitorContext().warn("The type " + type.getName() + " does not declare the " + kind + " '"
            + member + "'; the access is left to the resolvers at runtime", context.getOriginatingElement());
    }

    private static boolean isResolvedDynamically(ClassElement type) {
        return type.isArray()
            || type.isPrimitive()
            || type.getName().equals(Object.class.getName())
            || type.isAssignable(Map.class)
            || type.isAssignable(Collection.class)
            || type.isAssignable(ResourceBundle.class)
            || type.isAssignable(Optional.class)
            || type.isAssignable("jakarta.el.ELClass")
            || type.isAssignable("jakarta.el.LambdaExpression");
    }

    private Typed compileFunction(ELNode.Function function, ExpressionDef ctx) {
        List<List<ELNode>> invocations = function.invocations();
        List<ELNode> first = invocations.get(0);
        Typed result = compileFirstInvocation(function, first, ctx);
        for (int i = 1; i < invocations.size(); i++) {
            result = dynamic(runtime(EL_RESOLUTION, "invokeCallable", TypeDef.OBJECT,
                arguments(ctx, result.expression(), invocations.get(i))));
        }
        return result;
    }

    private Typed compileFirstInvocation(ELNode.Function function, List<ELNode> arguments, ExpressionDef ctx) {
        MethodElement declared = context.resolveFunction(function.prefix(), function.localName());
        if (declared != null) {
            if ((!declared.isVarArgs() && declared.getParameters().length != arguments.size())
                || (declared.isVarArgs() && arguments.size() < declared.getParameters().length - 1)) {
                throw new ELCompilationException("The function '"
                    + CompilationContext.qualifiedFunctionName(function.prefix(), function.localName())
                    + "' expects " + declared.getParameters().length + " argument(s) but "
                    + arguments.size() + " were given");
            }
            if (!ELTypes.isStatic(declared)) {
                // a function declared on a bean is invoked on the instance the context provides
                ClassElement bean = declared.getDeclaringType();
                ExpressionDef provider = runtime(EL_RESOLUTION, "functionProvider", TypeDef.erasure(bean),
                    ctx, ExpressionDef.constant(TypeDef.erasure(bean)));
                return new Typed(provider.invoke(declared, coercedArguments(declared, arguments, ctx)),
                    declared.getReturnType());
            }
            return new Typed(
                ELTypes.staticOwner(declared).invokeStatic(declared,
                    coercedArguments(declared, arguments, ctx)),
                declared.getReturnType()
            );
        }
        if (!function.prefix().isEmpty()) {
            throw new ELUndeclaredFunctionException(
                CompilationContext.qualifiedFunctionName(function.prefix(), function.localName()));
        }
        String name = function.localName();
        if (!context.isLambdaParameter(name) && context.variableType(name) == null) {
            ClassElement importedClass = context.resolveClass(name);
            if (importedClass != null) {
                MethodElement constructor = selectConstructor(importedClass, arguments, ctx);
                if (constructor == null) {
                    throw new ELCompilationException("The class " + importedClass.getName()
                        + " does not declare a public constructor accepting " + arguments.size()
                        + " argument(s)");
                }
                return new Typed(
                    ClassTypeDef.of(importedClass).instantiate(constructor,
                        coercedArguments(constructor, arguments, ctx)),
                    importedClass
                );
            }
            for (ClassElement staticImport : context.staticImports()) {
                MethodElement staticMethod = selectMethod(staticImport, name, arguments, true, ctx);
                if (staticMethod != null) {
                    return new Typed(
                        ClassTypeDef.of(staticMethod.getDeclaringType()).invokeStatic(staticMethod,
                            coercedArguments(staticMethod, arguments, ctx)),
                        staticMethod.getReturnType()
                    );
                }
            }
        }
        ExpressionDef target = runtime(EL_RESOLUTION, "resolveIdentifier", TypeDef.OBJECT,
            ctx, ExpressionDef.constant(name));
        return dynamic(runtime(EL_RESOLUTION, "invokeCallable", TypeDef.OBJECT,
            arguments(ctx, target, arguments)));
    }

    /**
     * The conditional operator keeps the type of the branch it selects, section 1.12 of the specification: the
     * branches of different types are boxed so that the Java conditional expression does not promote them to
     * a common numeric type.
     */
    private Typed compileTernary(ELNode.Ternary ternary, ExpressionDef ctx) {
        ExpressionDef condition = toBoolean(compileTyped(ternary.condition(), ctx));
        Typed ifTrue = compileTyped(ternary.ifTrue(), ctx);
        Typed ifFalse = compileTyped(ternary.ifFalse(), ctx);
        if (ifTrue.expression().type().equals(ifFalse.expression().type())) {
            ClassElement type = ifTrue.type() != null && ifFalse.type() != null
                && ifTrue.type().getName().equals(ifFalse.type().getName()) ? ifTrue.type() : null;
            return new Typed(new ExpressionDef.IfElse(condition, ifTrue.expression(), ifFalse.expression()), type);
        }
        return dynamic(new ExpressionDef.IfElse(condition, boxed(ifTrue.expression()), boxed(ifFalse.expression())));
    }

    /**
     * @return The expression as an {@link Object}: a primitive boxed through {@code valueOf}, which the Java
     * source writer renders without the cast a negative literal cannot take, then cast to Object so that the
     * Java conditional expression does not unbox two wrappers to a common numeric type
     */
    private static ExpressionDef boxed(ExpressionDef expression) {
        ExpressionDef reference = expression;
        if (expression.type() instanceof TypeDef.Primitive primitive) {
            reference = primitive.wrapperType().invokeStatic("valueOf", List.of(primitive), primitive.wrapperType(), List.of(expression));
        }
        return reference.cast(TypeDef.OBJECT);
    }

    private Typed compileUnary(ELNode.Unary unary, ExpressionDef ctx) {
        Typed operand = compileTyped(unary.operand(), ctx);
        return switch (unary.operator()) {
            case NEGATE -> negate(operand);
            case NOT -> isBoolean(operand)
                ? new Typed(new ExpressionDef.IsFalse(toBoolean(operand)), PrimitiveElement.BOOLEAN)
                : new Typed(runtime(EL_SUPPORT, "not", BOOLEAN, operand.expression()), PrimitiveElement.BOOLEAN);
            case EMPTY -> new Typed(runtime(EL_SUPPORT, "isEmpty", BOOLEAN, operand.expression()), PrimitiveElement.BOOLEAN);
        };
    }

    /**
     * The unary minus keeps the type of its operand, section 1.7.4 of the specification, so it is inlined for
     * the primitives Java negates in their own type and for the numeric literals, the narrower primitives
     * would widen to int.
     */
    private Typed negate(Typed operand) {
        TypeDef.Primitive type = numericType(operand);
        if (type != null && (type.equals(TypeDef.Primitive.LONG) || type.equals(TypeDef.Primitive.DOUBLE))) {
            return new Typed(numeric(operand, type).math(ExpressionDef.MathUnaryOperation.OpType.NEGATE),
                type.equals(TypeDef.Primitive.LONG) ? PrimitiveElement.LONG : PrimitiveElement.DOUBLE);
        }
        if (type != null && (type.equals(TypeDef.Primitive.INT) || type.equals(TypeDef.Primitive.FLOAT))) {
            return new Typed(operand.expression().math(ExpressionDef.MathUnaryOperation.OpType.NEGATE),
                type.equals(TypeDef.Primitive.INT) ? PrimitiveElement.INT : PrimitiveElement.FLOAT);
        }
        return dynamic(runtime(EL_ARITHMETIC, "negate", TypeDef.OBJECT, operand.expression()));
    }

    private Typed compileBinary(ELNode.Binary binary, ExpressionDef ctx) {
        if (binary.operator() == BinaryOperator.AND || binary.operator() == BinaryOperator.OR) {
            // the Java operators short-circuit the way the section 1.10 of the specification requires
            ExpressionDef.ConditionExpressionDef left = condition(toBoolean(compileTyped(binary.left(), ctx)));
            ExpressionDef.ConditionExpressionDef right = condition(toBoolean(compileTyped(binary.right(), ctx)));
            return bool(binary.operator() == BinaryOperator.AND ? left.and(right) : left.or(right));
        }
        Typed leftOperand = compileTyped(binary.left(), ctx);
        Typed rightOperand = compileTyped(binary.right(), ctx);
        if (binary.operator() == BinaryOperator.CONCAT) {
            return new Typed(concat(stringOperand(leftOperand), stringOperand(rightOperand)), ClassElement.of(String.class));
        }
        Typed inlined = inlineBinary(binary.operator(), leftOperand, rightOperand);
        if (inlined != null) {
            return inlined;
        }
        ExpressionDef left = leftOperand.expression();
        ExpressionDef right = rightOperand.expression();
        return switch (binary.operator()) {
            case ADD -> dynamic(runtime(EL_ARITHMETIC, "add", TypeDef.OBJECT, left, right));
            case SUBTRACT -> dynamic(runtime(EL_ARITHMETIC, "subtract", TypeDef.OBJECT, left, right));
            case MULTIPLY -> dynamic(runtime(EL_ARITHMETIC, "multiply", TypeDef.OBJECT, left, right));
            case DIVIDE -> dynamic(runtime(EL_ARITHMETIC, "divide", TypeDef.OBJECT, left, right));
            case MODULO -> dynamic(runtime(EL_ARITHMETIC, "mod", TypeDef.OBJECT, left, right));
            case EQUAL -> bool(runtime(EL_SUPPORT, "equals", BOOLEAN, left, right));
            case NOT_EQUAL -> bool(runtime(EL_SUPPORT, "notEquals", BOOLEAN, left, right));
            case LESS_THAN -> bool(runtime(EL_SUPPORT, "lessThan", BOOLEAN, left, right));
            case GREATER_THAN -> bool(runtime(EL_SUPPORT, "greaterThan", BOOLEAN, left, right));
            case LESS_THAN_OR_EQUAL -> bool(runtime(EL_SUPPORT, "lessThanOrEqual", BOOLEAN, left, right));
            case GREATER_THAN_OR_EQUAL -> bool(runtime(EL_SUPPORT, "greaterThanOrEqual", BOOLEAN, left, right));
            case AND, OR, CONCAT -> throw new IllegalStateException("The operator is compiled separately");
        };
    }

    /**
     * Compiles a binary operator to the Java operator the runtime would reach, when the types of both operands
     * are known at compilation time.
     *
     * <p>Two operands known to be numbers and not null skip the dispatch of the sections 1.7 and 1.9 of the
     * specification: they are coerced to {@code long}, or to {@code double} when one of them is a floating point
     * value or the operator is the division, and combined with the Java operator. The {@code double} comparisons
     * go through {@link Double#compare(double, double)}, which is how the runtime compares floating point
     * values, so that {@code NaN} and the signed zeros compare the same way. Two strings and two booleans are
     * compared for equality directly.</p>
     *
     * @return The compiled operation or {@code null} when the operands are not known well enough
     */
    @Nullable
    private Typed inlineBinary(BinaryOperator operator, Typed left, Typed right) {
        TypeDef.Primitive leftType = numericType(left);
        TypeDef.Primitive rightType = numericType(right);
        if (leftType != null && rightType != null) {
            boolean floating = operator == BinaryOperator.DIVIDE || leftType.isFloatNumber() || rightType.isFloatNumber();
            TypeDef.Primitive type = floating ? TypeDef.Primitive.DOUBLE : TypeDef.Primitive.LONG;
            ExpressionDef a = numeric(left, type);
            ExpressionDef b = numeric(right, type);
            return switch (operator) {
                case ADD -> arithmetic(ExpressionDef.MathBinaryOperation.OpType.ADDITION, a, b, floating);
                case SUBTRACT -> arithmetic(ExpressionDef.MathBinaryOperation.OpType.SUBTRACTION, a, b, floating);
                case MULTIPLY -> arithmetic(ExpressionDef.MathBinaryOperation.OpType.MULTIPLICATION, a, b, floating);
                case DIVIDE -> arithmetic(ExpressionDef.MathBinaryOperation.OpType.DIVISION, a, b, floating);
                case MODULO -> arithmetic(ExpressionDef.MathBinaryOperation.OpType.MODULUS, a, b, floating);
                case EQUAL -> comparison(ExpressionDef.ComparisonOperation.OpType.EQUAL_TO, a, b, floating);
                case NOT_EQUAL -> comparison(ExpressionDef.ComparisonOperation.OpType.NOT_EQUAL_TO, a, b, floating);
                case LESS_THAN -> comparison(ExpressionDef.ComparisonOperation.OpType.LESS_THAN, a, b, floating);
                case GREATER_THAN -> comparison(ExpressionDef.ComparisonOperation.OpType.GREATER_THAN, a, b, floating);
                case LESS_THAN_OR_EQUAL -> comparison(ExpressionDef.ComparisonOperation.OpType.LESS_THAN_OR_EQUAL, a, b, floating);
                case GREATER_THAN_OR_EQUAL -> comparison(ExpressionDef.ComparisonOperation.OpType.GREATER_THAN_OR_EQUAL, a, b, floating);
                case AND, OR, CONCAT -> null;
            };
        }
        if (operator == BinaryOperator.EQUAL || operator == BinaryOperator.NOT_EQUAL) {
            ExpressionDef equal = null;
            if (isString(left) && isString(right)) {
                equal = ClassTypeDef.of(Objects.class).invokeStatic("equals", List.of(TypeDef.OBJECT, TypeDef.OBJECT), BOOLEAN,
                    List.of(left.expression(), right.expression()));
            } else if (isBoolean(left) && isBoolean(right)) {
                equal = booleanEquality(toBoolean(left), toBoolean(right));
            }
            if (equal != null) {
                return bool(operator == BinaryOperator.EQUAL ? equal : new ExpressionDef.IsFalse(equal));
            }
        }
        return null;
    }

    /**
     * The equality of two booleans, which a literal operand reduces to the other operand or its negation.
     */
    private static ExpressionDef booleanEquality(ExpressionDef left, ExpressionDef right) {
        if (right instanceof ExpressionDef.Constant constant && constant.value() instanceof Boolean literal) {
            return literal ? left : new ExpressionDef.IsFalse(left);
        }
        if (left instanceof ExpressionDef.Constant constant && constant.value() instanceof Boolean literal) {
            return literal ? right : new ExpressionDef.IsFalse(right);
        }
        return left.compare(ExpressionDef.ComparisonOperation.OpType.EQUAL_TO, right);
    }

    private static Typed arithmetic(ExpressionDef.MathBinaryOperation.OpType operation, ExpressionDef a, ExpressionDef b, boolean floating) {
        return new Typed(a.math(operation, b), floating ? PrimitiveElement.DOUBLE : PrimitiveElement.LONG);
    }

    private static Typed comparison(ExpressionDef.ComparisonOperation.OpType operation, ExpressionDef a, ExpressionDef b, boolean floating) {
        if (floating) {
            ExpressionDef compared = ClassTypeDef.of(Double.class).invokeStatic("compare",
                List.of(TypeDef.Primitive.DOUBLE, TypeDef.Primitive.DOUBLE), TypeDef.Primitive.INT, List.of(a, b));
            return bool(compared.compare(operation, ExpressionDef.constant(0)));
        }
        return bool(a.compare(operation, b));
    }

    private static ExpressionDef.ConditionExpressionDef condition(ExpressionDef bool) {
        return bool instanceof ExpressionDef.ConditionExpressionDef condition ? condition : bool.isTrue();
    }

    private static Typed bool(ExpressionDef expression) {
        return new Typed(expression, PrimitiveElement.BOOLEAN);
    }

    /**
     * The primitive type of an operand the compiler knows to be a number and not null: a numeric primitive,
     * or a numeric literal. A wrapper may be null, which the operators treat as zero, and a string may hold a
     * number in any notation, both keep the coercions of the runtime.
     */
    private static TypeDef.@Nullable Primitive numericType(Typed operand) {
        if (operand.expression() instanceof ExpressionDef.Constant constant) {
            return switch (constant.value()) {
                case Long ignored -> TypeDef.Primitive.LONG;
                case Double ignored -> TypeDef.Primitive.DOUBLE;
                case null, default -> null;
            };
        }
        ClassElement type = operand.type();
        if (type == null || !type.isPrimitive() || !(operand.expression().type() instanceof TypeDef.Primitive primitive)) {
            return null;
        }
        return primitive.isNumber() && !primitive.equals(TypeDef.Primitive.CHAR) && !primitive.equals(TypeDef.Primitive.BOOLEAN) ? primitive : null;
    }

    /**
     * @return The numeric operand as the given primitive, a literal rewritten and a primitive widened
     */
    private static ExpressionDef numeric(Typed operand, TypeDef.Primitive type) {
        ExpressionDef expression = operand.expression();
        if (expression instanceof ExpressionDef.Constant constant && constant.value() instanceof Number number) {
            return type.equals(TypeDef.Primitive.DOUBLE) ? ExpressionDef.constant(number.doubleValue()) : ExpressionDef.constant(number.longValue());
        }
        return expression.type().equals(type) ? expression : expression.cast(type);
    }

    private static boolean isString(Typed operand) {
        return operand.type() != null && operand.type().getName().equals(String.class.getName());
    }

    /**
     * @return Whether the operand is known to be a boolean that is not null: a primitive or a literal
     */
    private static boolean isBoolean(Typed operand) {
        if (operand.expression() instanceof ExpressionDef.Constant constant) {
            return constant.value() instanceof Boolean;
        }
        return operand.type() != null && operand.type().isPrimitive() && operand.type().getName().equals("boolean");
    }

    private ExpressionDef compileAssign(ELNode.Assign assign, ExpressionDef ctx) {
        LValue lValue = compileLValue(assign.target(), ctx);
        if (lValue == null) {
            throw new ELCompilationException("The left side of an assignment must be an lvalue");
        }
        ExpressionDef value = compile(assign.value(), ctx);
        if (lValue.base() == null) {
            return runtime(EL_RESOLUTION, "assignIdentifier", TypeDef.OBJECT, ctx, lValue.property(), value);
        }
        return runtime(EL_RESOLUTION, "assignProperty", TypeDef.OBJECT,
            ctx, lValue.base(), lValue.property(), value);
    }

    /**
     * Compiles a lambda expression to a {@code jakarta.el.LambdaExpression}, the form it takes as a value: its
     * body is a Java lambda whose parameters are the parameters of the lambda expression, invoked with the
     * arguments directly, see {@code io.micronaut.el.runtime.CompiledLambdaExpression}. Up to three parameters
     * are passed as such, more are passed as an array.
     */
    private ExpressionDef compileLambda(ELNode.Lambda lambda, ExpressionDef ctx) {
        List<String> parameters = lambda.parameters();
        int arity = parameters.size();
        Class<?> bodyType = switch (arity) {
            case 0 -> ELLambdaBody.Nullary.class;
            case 1 -> ELLambdaBody.Unary.class;
            case 2 -> ELLambdaBody.Binary.class;
            case 3 -> ELLambdaBody.Ternary.class;
            default -> ELLambdaBody.class;
        };
        ClassElement bodyElement = elementOf(bodyType);
        MethodElement evaluate = Objects.requireNonNull(functionalMethod(bodyElement));
        ExpressionDef body = javaLambda(null, bodyElement, evaluate, parameters, lambda.body(), null, arity > 3);
        List<ExpressionDef> arguments = new ArrayList<>(arity + 2);
        arguments.add(ctx);
        if (arity > 3) {
            // the names are passed as an array: a generic List.of has a descriptor neither writer infers alike
            arguments.add(STRING.array().instantiate(parameters.stream()
                .map(parameter -> (ExpressionDef) ExpressionDef.constant(parameter))
                .toList()));
            arguments.add(body);
            return runtime(EL_LAMBDAS, "lambda", LAMBDA_EXPRESSION, arguments);
        }
        parameters.forEach(parameter -> arguments.add(ExpressionDef.constant(parameter)));
        arguments.add(body);
        return runtime(EL_LAMBDAS, "lambda" + arity, LAMBDA_EXPRESSION, arguments);
    }

    /**
     * Compiles a lambda expression passed to a method whose parameter is a functional interface to a Java lambda
     * implementing the interface: the coercion of the section 1.25.8 of the specification, without the
     * {@code LambdaExpression} and the proxy the runtime coercion goes through. The body evaluates with the
     * enclosing context, which is the context the runtime coercion would set on the lambda expression.
     */
    private ExpressionDef functionalLambda(ELNode.Lambda lambda, @Nullable ClassElement receiver, ClassElement functionalInterface, MethodElement method, ExpressionDef ctx) {
        if (lambda.parameters().size() > method.getParameters().length) {
            throw new ELCompilationException("The lambda expression declares " + lambda.parameters().size()
                + " parameter(s) but " + functionalInterface.getName() + "." + method.getName()
                + " passes " + method.getParameters().length);
        }
        return javaLambda(receiver, functionalInterface, method, lambda.parameters(), lambda.body(), ctx, false);
    }

    /**
     * Compiles the body of a lambda expression as a Java lambda implementing a functional interface.
     *
     * <p>The parameters of the lambda expression are bound to the Java parameters of the method, in order, and
     * the identifiers of the body naming them compile to the parameters, cast to the type the interface declares
     * for them when it is known. The result of the body is coerced to the return type of the method.</p>
     *
     * @param receiver            The type declaring the method taking the lambda, binding its type variables
     * @param functionalInterface The interface the lambda implements
     * @param method              Its functional method
     * @param parameters          The parameters of the lambda expression
     * @param body                The body of the lambda expression
     * @param enclosingContext    The context the body evaluates with, or {@code null} when the first parameter
     *                            of the method is the context the lambda expression is invoked with
     * @param arrayArguments      Whether the method passes the arguments as an array, its last parameter
     * @return The Java lambda
     */
    private ExpressionDef javaLambda(@Nullable ClassElement receiver,
                                     ClassElement functionalInterface,
                                     MethodElement method,
                                     List<String> parameters,
                                     ELNode body,
                                     @Nullable ExpressionDef enclosingContext,
                                     boolean arrayArguments) {
        int index = lambdas++;
        ParameterElement[] methodParameters = method.getParameters();
        int offset = enclosingContext == null ? 1 : 0;
        MethodDef.MethodDefBuilder builder = MethodDef.builder(method.getName()).addModifiers(Modifier.PUBLIC);
        for (int i = 0; i < methodParameters.length; i++) {
            String javaName;
            if (enclosingContext == null && i == 0) {
                javaName = "elContext" + index;
            } else if (arrayArguments) {
                javaName = "arguments" + index;
            } else if (i - offset < parameters.size()) {
                // suffixed so that the parameters of the nested lambda expressions do not shadow each other
                javaName = parameters.get(i - offset) + "_" + index;
            } else {
                javaName = "unused" + index + "_" + i;
            }
            builder.addParameter(javaName, erasure(methodParameters[i].getType()));
        }
        ClassElement returnType = method.getReturnType();
        builder.returns(erasure(returnType));
        MethodDef implementation = builder.build((aThis, javaParameters) -> {
            ExpressionDef bodyContext = enclosingContext != null ? captured(enclosingContext) : captured(javaParameters.get(0));
            Map<String, Typed> scope = new LinkedHashMap<>();
            for (int i = 0; i < parameters.size(); i++) {
                if (arrayArguments) {
                    ExpressionDef array = captured(javaParameters.get(javaParameters.size() - 1));
                    scope.put(parameters.get(i), dynamic(runtime(EL_LAMBDAS, "argument", TypeDef.OBJECT, array, ExpressionDef.constant(i))));
                } else {
                    scope.put(parameters.get(i), typedParameter(captured(javaParameters.get(i + offset)), receiver, functionalInterface, methodParameters[i + offset].getType()));
                }
            }
            context.enterLambdaScope(scope, enclosingContext != null);
            try {
                return lambdaResult(compileTyped(body, bodyContext), returnType, bodyContext);
            } finally {
                context.exitLambdaScope();
            }
        });
        return new ExpressionDef.Lambda(ClassTypeDef.of(functionalInterface), MethodDef.of(method), implementation);
    }

    /**
     * The erasure of a type, a type variable erasing to its first bound and a wildcard to {@link Object}:
     * {@link TypeDef#erasure} keeps the variables of the functional interfaces, which the writers cannot render.
     */
    private static TypeDef erasure(ClassElement type) {
        if (type instanceof GenericPlaceholderElement placeholder) {
            List<? extends ClassElement> bounds = placeholder.getBounds();
            return bounds.isEmpty() ? TypeDef.OBJECT : erasure(bounds.get(0));
        }
        if (type.isWildcard() || type.isTypeVariable()) {
            return TypeDef.OBJECT;
        }
        return TypeDef.erasure(type);
    }

    /**
     * A variable a nested Java lambda may capture, referenced as a local: inside a lambda both writers of
     * SourceGen 2.1.0 resolve a local by its name, while the Java source writer rejects a parameter of an
     * enclosing method.
     */
    private static ExpressionDef captured(ExpressionDef variable) {
        if (variable instanceof VariableDef.MethodParameter parameter) {
            return new VariableDef.Local(parameter.name(), parameter.type());
        }
        return variable;
    }

    /**
     * @return The Java parameter as the type the functional interface declares for it, when the type is known
     */
    private static Typed typedParameter(ExpressionDef parameter, @Nullable ClassElement receiver, ClassElement functionalInterface, ClassElement declared) {
        ClassElement type = declared;
        if (type instanceof GenericPlaceholderElement placeholder && placeholder.getResolved().isEmpty()) {
            type = functionalInterface.getTypeArguments().get(placeholder.getVariableName());
        }
        if (type instanceof WildcardElement wildcard) {
            // the argument of a functional interface parameter is a super-type wildcard, ? super T, whose
            // lower bound is the type the lambda receives; an extends wildcard receives at most its upper bound
            List<? extends ClassElement> bounds = wildcard.getLowerBounds().isEmpty() ? wildcard.getUpperBounds() : wildcard.getLowerBounds();
            type = bounds.isEmpty() ? null : bounds.get(0);
        }
        if (type instanceof GenericPlaceholderElement placeholder && placeholder.getResolved().isEmpty() && receiver != null) {
            // a type variable of the class declaring the method, which the type arguments of the receiver bind
            type = receiver.getTypeArguments().get(placeholder.getVariableName());
        }
        type = resolved(type);
        if (type == null || type.isPrimitive() || isUnknown(type)) {
            return new Typed(parameter, declared.isPrimitive() ? declared : null);
        }
        return new Typed(parameter.cast(erasure(type)), type);
    }

    /**
     * @return The result of a lambda body as the return type of the functional method, a statement
     */
    private StatementDef lambdaResult(Typed result, ClassElement returnType, ExpressionDef ctx) {
        if (returnType.getName().equals("void")) {
            // returned rather than a statement: the Java source writer of SourceGen 2.1.0 renders a lambda
            // with a statement body inside a statement as nested statements, which JavaPoet rejects
            return runtime(EL_LAMBDAS, "discard", TypeDef.VOID, result.expression()).returning();
        }
        if (returnType.getName().equals("boolean")) {
            return toBoolean(result).returning();
        }
        if (returnType.isTypeVariable() || returnType.getName().equals(Object.class.getName())) {
            // returned as Object, the erasure: a typed result would let javac infer a narrower type argument
            // for the invocation, which the coerced arguments of the following invocations do not fit
            return result.expression().cast(TypeDef.OBJECT).returning();
        }
        ClassElement type = result.type();
        if (type != null && (type.getName().equals(returnType.getName())
            || returnType.isPrimitive() && wrapper(returnType.getName()).equals(type.getName())
            || type.isPrimitive() && wrapper(type.getName()).equals(returnType.getName()))) {
            // the body already has the type the interface returns
            ExpressionDef value = result.expression();
            return (value.type().equals(erasure(returnType)) ? value : value.cast(erasure(returnType))).returning();
        }
        return coerce(result.expression(), returnType, ctx).returning();
    }

    /**
     * @param type A type
     * @return The single abstract method of a functional interface, or {@code null} when the type is not one
     */
    @Nullable
    private static MethodElement functionalMethod(ClassElement type) {
        if (!type.isInterface() || type.isAssignable(LambdaExpression.class)) {
            return null;
        }
        MethodElement found = null;
        Set<String> seen = new java.util.HashSet<>();
        for (MethodElement method : type.getEnclosedElements(ElementQuery.ALL_METHODS.onlyAbstract())) {
            if (OBJECT_METHODS.contains(method.getName() + "/" + method.getParameters().length)
                || !seen.add(method.getName() + "/" + method.getParameters().length)) {
                continue;
            }
            if (found != null) {
                return null;
            }
            found = method;
        }
        return found;
    }

    private ClassElement elementOf(Class<?> type) {
        return context.getVisitorContext().getClassElement(type)
            .orElseThrow(() -> new ELCompilationException("Cannot resolve the type " + type.getName()
                + ", the module is missing from the compilation classpath"));
    }

    private List<ExpressionDef> compileMapEntries(ELNode.MapData map, ExpressionDef ctx) {
        List<ExpressionDef> values = new ArrayList<>(map.entries().size() * 2);
        for (ELNode.MapData.MapEntry entry : map.entries()) {
            ELNode value = entry.value();
            if (value == null) {
                throw new ELCompilationException("A map construction requires a value for every key");
            }
            values.add(compile(entry.key(), ctx));
            values.add(compile(value, ctx));
        }
        return values;
    }

    private List<ExpressionDef> compileAll(List<ELNode> nodes, ExpressionDef ctx) {
        return nodes.stream().map(node -> compile(node, ctx)).toList();
    }

    private List<ExpressionDef> arguments(ExpressionDef ctx, ExpressionDef target, List<ELNode> arguments) {
        List<ExpressionDef> values = new ArrayList<>(arguments.size() + 2);
        values.add(ctx);
        values.add(target);
        values.addAll(compileAll(arguments, ctx));
        return values;
    }

    private List<ExpressionDef> arguments(ExpressionDef ctx,
                                          ExpressionDef base,
                                          ExpressionDef property,
                                          List<ELNode> arguments) {
        List<ExpressionDef> values = new ArrayList<>(arguments.size() + 3);
        values.add(ctx);
        values.add(base);
        values.add(property);
        values.addAll(compileAll(arguments, ctx));
        return values;
    }

    private List<ExpressionDef> coercedArguments(MethodElement method, List<ELNode> arguments, ExpressionDef ctx) {
        return coercedArguments(null, method, arguments, ctx);
    }

    /**
     * @param receiver The type the method is invoked on, whose type arguments bind the type variables of the
     *                 parameters, or {@code null} for a static method
     */
    private List<ExpressionDef> coercedArguments(@Nullable ClassElement receiver, MethodElement method, List<ELNode> arguments, ExpressionDef ctx) {
        ParameterElement[] parameters = method.getParameters();
        List<ExpressionDef> values = new ArrayList<>(arguments.size());
        boolean directVarargsArray = false;
        for (int i = 0; i < arguments.size(); i++) {
            ELNode argument = arguments.get(i);
            ClassElement parameter = i < parameters.length ? parameters[i].getType() : null;
            Typed directValue = null;
            if (method.isVarArgs() && i == parameters.length - 1 && arguments.size() == parameters.length
                && !(argument instanceof ELNode.Lambda)) {
                directValue = compileTyped(argument, ctx);
                if (directValue.type() != null && directValue.type().isArray()) {
                    directVarargsArray = true;
                } else {
                    parameter = parameters[parameters.length - 1].getType().fromArray();
                }
            } else if (method.isVarArgs() && i >= parameters.length - 1) {
                parameter = parameters[parameters.length - 1].getType().fromArray();
            }
            if (argument instanceof ELNode.Lambda lambda && parameter != null) {
                MethodElement functionalMethod = functionalMethod(parameter);
                if (functionalMethod != null) {
                    // the section 1.25.8 of the specification coerces a lambda expression to a functional
                    // interface, which a lambda expression written in place implements directly
                    values.add(functionalLambda(lambda, receiver, parameter, functionalMethod, ctx));
                    continue;
                }
                if (parameter.isAssignable(LambdaExpression.class)) {
                    values.add(compileLambda(lambda, ctx));
                    continue;
                }
            }
            Typed value = directValue == null ? compileTyped(argument, ctx) : directValue;
            if (parameter != null) {
                requireCoercible(value.type(), parameter, method, i);
            }
            values.add(parameter == null ? value.expression() : coerce(value.expression(), parameter, ctx));
        }
        if (!method.isVarArgs() || directVarargsArray) {
            return values;
        }
        int fixed = parameters.length - 1;
        List<ExpressionDef> packed = new ArrayList<>(parameters.length);
        packed.addAll(values.subList(0, fixed));
        ClassElement component = parameters[fixed].getType().fromArray();
        packed.add(erasure(component).array().instantiate(values.subList(fixed, values.size())));
        return packed;
    }

    /**
     * An argument whose static type cannot be coerced to the parameter, section 1.23 of the specification, is
     * an error: the invocation would fail on every evaluation.
     */
    private static void requireCoercible(@Nullable ClassElement argument, ClassElement parameter, MethodElement method, int index) {
        if (argument == null || isCoercible(argument, parameter)) {
            return;
        }
        throw new ELCompilationException("The argument " + (index + 1) + " of "
            + method.getDeclaringType().getName() + "." + method.getName() + " is a " + argument.getName()
            + ", which cannot be coerced to the declared " + parameter.getName()
            + ". Pass a compatible value, or change the signature of the method.");
    }

    /**
     * Whether a value of the type can be coerced to the parameter type: assignable, or one of the coercions of
     * the section 1.23. A string coerces to almost anything, including through a {@code PropertyEditor}, so a
     * string argument is never rejected.
     */
    private static boolean isCoercible(ClassElement argument, ClassElement parameter) {
        if (parameter.isArray()) {
            // a variable arity parameter, or an array the runtime coercion of the section 1.23.6 fills
            return true;
        }
        if (isUnknown(parameter) || isUnknown(argument)
            || argument.getName().equals(parameter.getName())
            || argument.isAssignable(parameter.getName())
            || argument.getName().equals(String.class.getName())
            || parameter.getName().equals(String.class.getName())
            || parameter.isAssignable(argument.getName())) {
            return true;
        }
        boolean argumentNumeric = numericRank(argument) >= 0 || argument.isAssignable(Number.class)
            || argument.getName().equals("char") || argument.isAssignable(Character.class);
        boolean parameterNumeric = numericRank(parameter) >= 0 || parameter.isAssignable(Number.class)
            || parameter.getName().equals("char") || parameter.isAssignable(Character.class);
        if (argumentNumeric || parameterNumeric) {
            return argumentNumeric && parameterNumeric;
        }
        boolean argumentBoolean = argument.getName().equals("boolean") || argument.isAssignable(Boolean.class);
        boolean parameterBoolean = parameter.getName().equals("boolean") || parameter.isAssignable(Boolean.class);
        if (argumentBoolean || parameterBoolean) {
            return argumentBoolean && parameterBoolean;
        }
        if (parameter.isEnum()) {
            // only a string, handled above, coerces to an enum
            return false;
        }
        if (parameter.isInterface() && argument.isAssignable(LambdaExpression.class)) {
            return true;
        }
        // an unrelated reference type: the coercion of the section 1.23.10 requires an instance of the type
        return false;
    }

    private ExpressionDef coerce(ExpressionDef value, ClassElement target, ExpressionDef ctx) {
        TypeDef targetType = erasure(target);
        if (targetType.equals(TypeDef.OBJECT)) {
            // the coercion to Object is the identity
            return value;
        }
        ExpressionDef literal = coerceLiteral(value, target);
        if (literal != null) {
            return literal;
        }
        // the section 1.25.8 of the specification coerces a lambda expression to a functional interface
        boolean functionalInterface = target.isInterface() && !target.isAssignable(LambdaExpression.class);
        return runtime(EL_SUPPORT, 
            functionalInterface ? "coerceToFunctionalInterface" : "coerceToType",
            targetType,
            ctx, value, ExpressionDef.constant(targetType)
        );
    }

    /**
     * Coerces a literal to a type at compilation time, as described in the section 1.23 of the specification:
     * a number to a numeric type, a string to a string, a boolean to a boolean.
     *
     * @return The coerced literal, or {@code null} when the value is not a literal the compiler coerces
     */
    @Nullable
    private static ExpressionDef coerceLiteral(ExpressionDef value, ClassElement target) {
        if (!(value instanceof ExpressionDef.Constant constant) || constant.value() == null) {
            return null;
        }
        Object literal = constant.value();
        String name = target.getName();
        if (literal instanceof String && name.equals(String.class.getName())
            || literal instanceof Boolean && (name.equals("boolean") || name.equals(Boolean.class.getName()))) {
            return value;
        }
        if (literal instanceof Long || literal instanceof Double) {
            Number number = (Number) literal;
            return switch (name) {
                case "byte" -> ExpressionDef.primitiveConstant(number.byteValue());
                case "short" -> ExpressionDef.primitiveConstant(number.shortValue());
                case "int" -> ExpressionDef.constant(number.intValue());
                case "long" -> ExpressionDef.constant(number.longValue());
                case "float" -> ExpressionDef.constant(number.floatValue());
                case "double" -> ExpressionDef.constant(number.doubleValue());
                case "java.lang.Byte" -> ExpressionDef.constant(Byte.valueOf(number.byteValue()));
                case "java.lang.Short" -> ExpressionDef.constant(Short.valueOf(number.shortValue()));
                case "java.lang.Integer" -> ExpressionDef.constant(Integer.valueOf(number.intValue()));
                case "java.lang.Long" -> ExpressionDef.constant(Long.valueOf(number.longValue()));
                case "java.lang.Float" -> ExpressionDef.constant(Float.valueOf(number.floatValue()));
                case "java.lang.Double" -> ExpressionDef.constant(Double.valueOf(number.doubleValue()));
                default -> null;
            };
        }
        return null;
    }

    /**
     * Coerces a value to a {@code boolean} as described in the section 1.23.4 of the specification, directly
     * when it is known to be a boolean.
     */
    private ExpressionDef toBoolean(Typed value) {
        if (value.expression() instanceof ExpressionDef.Constant constant && constant.value() instanceof Boolean bool) {
            return ExpressionDef.constant(bool.booleanValue());
        }
        if (isBoolean(value)) {
            return value.expression();
        }
        return runtime(EL_SUPPORT, "toBoolean", BOOLEAN, value.expression());
    }

    private static boolean isStreamable(ClassElement type) {
        return type.isArray() || type.isAssignable(Collection.class);
    }

    /**
     * @param source The type of the source of a stream, an array or a collection
     * @return The type of the stream, with the type of its elements when the source declares it
     */
    private ClassElement streamOf(ClassElement source) {
        ClassElement stream = elementOf(ELStream.class);
        ClassElement element = source.isArray() ? source.fromArray() : resolved(source.getTypeArguments(Iterable.class).get("T"));
        if (element == null || element.isPrimitive() || isUnknown(element)) {
            return stream;
        }
        return stream.withTypeArguments(List.of(element));
    }

    /**
     * @return The return type of a method invoked on a receiver, a type variable of the declaring class bound
     * by the type arguments of the receiver
     */
    private static ClassElement returnType(ClassElement receiver, MethodElement method) {
        ClassElement returnType = method.getReturnType();
        Map<String, ClassElement> bindings = receiver.getTypeArguments();
        if (returnType instanceof GenericPlaceholderElement placeholder) {
            ClassElement bound = placeholder.getResolved().orElse(bindings.get(placeholder.getVariableName()));
            return bound != null && !isUnknown(bound) ? bound : returnType;
        }
        Map<String, ClassElement> arguments = returnType.getTypeArguments();
        if (arguments.isEmpty()) {
            return returnType;
        }
        Map<String, ClassElement> resolved = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<String, ClassElement> argument : arguments.entrySet()) {
            ClassElement value = argument.getValue();
            if (value instanceof GenericPlaceholderElement placeholder && placeholder.getResolved().isEmpty()) {
                ClassElement bound = bindings.get(placeholder.getVariableName());
                if (bound != null && !isUnknown(bound)) {
                    value = bound;
                    changed = true;
                }
            }
            resolved.put(argument.getKey(), value);
        }
        return changed ? returnType.withTypeArguments(resolved) : returnType;
    }

    /**
     * @return Whether a type tells nothing of the values it describes: a type variable, a wildcard or Object
     */
    private static boolean isUnknown(ClassElement type) {
        return type.isTypeVariable() || type.isWildcard() || type.getName().equals(Object.class.getName());
    }

    /**
     * @return The type a placeholder is resolved to, {@code List<String>} reporting its element as the type
     * variable {@code E} resolved to {@code String}, or the type itself
     */
    @Nullable
    private static ClassElement resolved(@Nullable ClassElement type) {
        if (type instanceof GenericPlaceholderElement placeholder) {
            return placeholder.getResolved().orElse(type);
        }
        return type;
    }

    private static ExpressionDef elClass(ClassElement type) {
        return EL_CLASS.instantiate(ExpressionDef.constant(TypeDef.erasure(type)));
    }

    private Typed staticFieldAccess(FieldElement field) {
        ClassElement declaringType = field.getDeclaringType();
        return new Typed(
            ClassTypeDef.of(declaringType).getStaticField(field.getName(), TypeDef.erasure(field.getType())),
            field.getType()
        );
    }

    @Nullable
    private static String constantName(ELNode property) {
        return property instanceof ELNode.StringLiteral literal ? literal.value() : null;
    }

    @Nullable
    private static FieldElement findStaticField(ClassElement type, String name) {
        return type.getEnclosedElements(ElementQuery.ALL_FIELDS.includeEnumConstants().named(name)).stream()
            .filter(ELTypes::isPublicStatic)
            .findFirst()
            .orElse(null);
    }

    @Nullable
    private static MethodElement findPropertyReader(ClassElement type, String name) {
        for (PropertyElement property : type.getBeanProperties()) {
            if (property.getName().equals(name)) {
                Optional<MethodElement> readMethod = property.getReadMethod();
                if (readMethod.isPresent() && readMethod.get().isPublic()) {
                    return readMethod.get();
                }
                return null;
            }
        }
        return null;
    }

    /**
     * The method of the given name taking the arguments. Among several overloads of the right arity the one
     * whose parameters fit the static types of the arguments best is selected, exact over widening over
     * coercible; when two fit equally well, or an argument has no static type to decide by, the resolution
     * is left to the resolvers at runtime.
     */
    @Nullable
    private MethodElement selectMethod(ClassElement type, String name, List<ELNode> arguments, boolean onlyStatic, ExpressionDef ctx) {
        List<MethodElement> candidates = new ArrayList<>();
        for (MethodElement method : type.getEnclosedElements(ElementQuery.ALL_METHODS.onlyAccessible().named(name))) {
            if ((method.getParameters().length == arguments.size()
                || method.isVarArgs() && arguments.size() >= method.getParameters().length - 1) && method.isPublic()
                && (!onlyStatic || ELTypes.isStatic(method))) {
                candidates.add(method);
            }
        }
        if (candidates.size() <= 1) {
            return candidates.isEmpty() ? null : candidates.get(0);
        }
        List<ClassElement> argumentTypes = arguments.stream()
            .map(argument -> argument instanceof ELNode.Lambda ? null : compileTyped(argument, ctx).type())
            .toList();
        MethodElement best = null;
        int bestScore = Integer.MAX_VALUE;
        boolean ambiguous = false;
        for (MethodElement candidate : candidates) {
            int score = score(candidate, arguments, argumentTypes);
            if (score < 0) {
                continue;
            }
            if (score < bestScore) {
                best = candidate;
                bestScore = score;
                ambiguous = false;
            } else if (score == bestScore) {
                ambiguous = true;
            }
        }
        return ambiguous ? null : best;
    }

    private static int score(MethodElement method, List<ELNode> arguments, List<ClassElement> argumentTypes) {
        ParameterElement[] parameters = method.getParameters();
        int fixed = method.isVarArgs() ? parameters.length - 1 : parameters.length;
        if (method.isVarArgs() ? arguments.size() < fixed : arguments.size() != fixed) {
            return -1;
        }
        int total = method.isVarArgs() ? 1 << 20 : 0;
        for (int i = 0; i < arguments.size(); i++) {
            ClassElement parameter = method.isVarArgs() && i >= fixed
                ? parameters[parameters.length - 1].getType().fromArray() : parameters[i].getType();
            if (arguments.get(i) instanceof ELNode.Lambda) {
                // a lambda expression is compiled to a functional interface, section 1.25.8, and is itself a
                // LambdaExpression, the former being the direct form
                if (functionalMethod(parameter) != null) {
                    total += EXACT;
                } else if (parameter.isAssignable(LambdaExpression.class) || parameter.getName().equals(Object.class.getName())) {
                    total += WIDENING;
                } else {
                    return -1;
                }
                continue;
            }
            ClassElement argument = argumentTypes.get(i);
            if (argument == null) {
                return -1;
            }
            int parameterRank = numericRank(parameter);
            int argumentRank = numericRank(argument);
            if (parameterRank >= 0 && argumentRank >= 0) {
                total += parameterRank == argumentRank ? EXACT : parameterRank > argumentRank ? WIDENING : COERCIBLE;
            } else if (argument.isAssignable(parameter) || parameter.getName().equals(Object.class.getName())) {
                total += EXACT;
            } else if (argument.isAssignable(String.class) || parameterRank >= 0 || parameter.isAssignable(String.class)) {
                total += COERCIBLE;
            } else {
                return -1;
            }
        }
        return total;
    }

    /**
     * @return The width of a numeric type, so that a wider parameter accepts a narrower argument, or -1
     */
    private static int numericRank(ClassElement type) {
        return switch (type.getName()) {
            case "byte", "java.lang.Byte" -> 0;
            case "short", "java.lang.Short" -> 1;
            case "int", "java.lang.Integer" -> 2;
            case "long", "java.lang.Long" -> 3;
            case "float", "java.lang.Float" -> 4;
            case "double", "java.lang.Double" -> 5;
            default -> -1;
        };
    }

    private static Class<?> integerLiteralType(String image) {
        try {
            Long.parseLong(image);
            return Long.class;
        } catch (NumberFormatException e) {
            return BigInteger.class;
        }
    }

    private static Class<?> floatingPointLiteralType(String image) {
        return Double.isInfinite(Double.parseDouble(image)) ? BigDecimal.class : Double.class;
    }

    @Nullable
    private MethodElement selectConstructor(ClassElement type, List<ELNode> arguments, ExpressionDef ctx) {
        List<MethodElement> candidates = new ArrayList<>();
        for (MethodElement constructor : type.getEnclosedElements(ElementQuery.CONSTRUCTORS)) {
            if (constructor.isPublic() && (constructor.getParameters().length == arguments.size()
                || constructor.isVarArgs() && arguments.size() >= constructor.getParameters().length - 1)) {
                candidates.add(constructor);
            }
        }
        if (candidates.size() <= 1) {
            return candidates.isEmpty() ? null : candidates.get(0);
        }
        List<ClassElement> types = arguments.stream().map(argument -> argument instanceof ELNode.Lambda ? null : compileTyped(argument, ctx).type()).toList();
        MethodElement best = null;
        int bestScore = Integer.MAX_VALUE;
        for (MethodElement candidate : candidates) {
            int score = score(candidate, arguments, types);
            if (score >= 0 && score < bestScore) {
                best = candidate;
                bestScore = score;
            } else if (score == bestScore) {
                best = null;
            }
        }
        return best;
    }

    private static ExpressionDef integerLiteral(String image) {
        try {
            return ExpressionDef.constant(Long.valueOf(image));
        } catch (NumberFormatException e) {
            return ClassTypeDef.of(BigInteger.class).instantiate(ExpressionDef.constant(image));
        }
    }

    private static ExpressionDef floatingPointLiteral(String image) {
        double value = Double.parseDouble(image);
        if (Double.isInfinite(value)) {
            return ClassTypeDef.of(BigDecimal.class).instantiate(ExpressionDef.constant(image));
        }
        return ExpressionDef.constant(Double.valueOf(value));
    }

    private static Typed dynamic(ExpressionDef expression) {
        return new Typed(expression, null);
    }

    /**
     * A compiled expression with the type known at compilation time, when there is one.
     *
     * @param expression The compiled expression
     * @param type       The statically known type, {@code null} when the expression is resolved dynamically
     */
    public record Typed(ExpressionDef expression, @Nullable ClassElement type) {
    }

    /**
     * The base object and the property of an lvalue.
     *
     * @param base     The base object, {@code null} when the lvalue is a single identifier
     * @param property The property, or the name of the identifier
     */
    public record LValue(@Nullable ExpressionDef base, ExpressionDef property) {
    }
}
