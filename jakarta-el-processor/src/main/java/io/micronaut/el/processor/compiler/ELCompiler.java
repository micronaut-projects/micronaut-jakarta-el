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
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.LambdaDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.TypeDef;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.LambdaExpression;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.Optional;

/**
 * The compiler lowering the abstract syntax tree of an expression to the source model of Micronaut
 * SourceGen.
 *
 * <p>Every construct of the language is compiled to the equivalent Java expression. The property
 * accesses, the method invocations, the static references and the functions whose types are known at
 * compilation time are compiled to direct invocations, the remaining ones are compiled to the resolution
 * described in the sections 1.5 and 1.6 of the specification.</p>
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
    private static final ClassTypeDef EL_CONTEXT = ClassTypeDef.of(ELContext.class);
    private static final TypeDef LAMBDA_EXPRESSION = TypeDef.of(LambdaExpression.class);
    private static final TypeDef STRING = TypeDef.of(String.class);
    private static final TypeDef BOOLEAN = TypeDef.Primitive.BOOLEAN;
    private static final int EXACT = 0;
    private static final int WIDENING = 1;
    private static final int COERCIBLE = 3;

    private final CompilationContext context;
    private int lambdas;

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

    private Typed compileTyped(ELNode node, ExpressionDef ctx) {
        return switch (node) {
            case ELNode.Eval eval -> compileTyped(eval.expression(), ctx);
            case ELNode.Composite composite -> compileComposite(composite, ctx);
            case ELNode.LiteralText literal -> new Typed(ExpressionDef.constant(literal.text()), null);
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
            case ELNode.Ternary ternary -> dynamic(new ExpressionDef.IfElse(
                toBoolean(compile(ternary.condition(), ctx)),
                compile(ternary.ifTrue(), ctx),
                compile(ternary.ifFalse(), ctx)
            ));
            case ELNode.Assign assign -> dynamic(compileAssign(assign, ctx));
            case ELNode.Semicolon semicolon -> dynamic(runtime(EL_SUPPORT, "sequence", TypeDef.OBJECT,
                compile(semicolon.left(), ctx), compile(semicolon.right(), ctx)));
            case ELNode.Lambda lambda -> dynamic(compileLambda(lambda, ctx));
            case ELNode.ListData list -> dynamic(runtime(EL_COLLECTIONS, "list", TypeDef.OBJECT,
                compileAll(list.elements(), ctx)));
            case ELNode.SetData set -> dynamic(runtime(EL_COLLECTIONS, "set", TypeDef.OBJECT,
                compileAll(set.elements(), ctx)));
            case ELNode.MapData map -> dynamic(runtime(EL_COLLECTIONS, "map", TypeDef.OBJECT,
                compileMapEntries(map, ctx)));
        };
    }

    private Typed compileComposite(ELNode.Composite composite, ExpressionDef ctx) {
        ExpressionDef result = null;
        for (ELNode part : composite.parts()) {
            ExpressionDef compiled = compile(part, ctx);
            result = result == null ? compiled : runtime(EL_ARITHMETIC, "concat", STRING, result, compiled);
        }
        if (result == null) {
            return new Typed(ExpressionDef.constant(""), null);
        }
        return new Typed(result, null);
    }

    private Typed compileIdentifier(ELNode.Identifier identifier, ExpressionDef ctx) {
        String name = identifier.name();
        ClassElement variableType = context.variableType(name);
        ExpressionDef resolved = runtime(EL_RESOLUTION, "resolveIdentifier", TypeDef.OBJECT,
            ctx, ExpressionDef.constant(name));
        if (variableType != null) {
            return new Typed(resolved.cast(TypeDef.erasure(variableType)), variableType);
        }
        if (!context.isLambdaParameter(name)) {
            FieldElement staticField = context.resolveStaticField(name);
            if (staticField != null) {
                return staticFieldAccess(staticField);
            }
        }
        return dynamic(resolved);
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
        if (propertyName != null && base.type() != null) {
            MethodElement reader = findPropertyReader(base.type(), propertyName);
            if (reader != null) {
                return new Typed(base.expression().invoke(reader), reader.getReturnType());
            }
            warnUnknownMember(base.type(), "property", propertyName);
        }
        return dynamic(runtime(EL_RESOLUTION, "getValue", TypeDef.OBJECT,
            ctx, base.expression(), compile(property.property(), ctx)));
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
        if (STREAM.equals(methodName) && method.arguments().isEmpty() && isStreamable(base.type())) {
            // the section 2.3.1 of the specification defines stream() as the source of a pipeline
            return dynamic(runtime(EL_STREAM, "of", TypeDef.OBJECT, ctx, base.expression()));
        }
        if (methodName != null && base.type() != null) {
            MethodElement target = selectMethod(base.type(), methodName, method.arguments(), false, ctx);
            if (target != null) {
                return new Typed(
                    base.expression().invoke(target, coercedArguments(target, method.arguments(), ctx)),
                    target.getReturnType()
                );
            }
            warnUnknownMember(base.type(), "method", methodName + "(" + method.arguments().size() + " argument(s))");
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
            if (declared.getParameters().length != arguments.size()) {
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
                MethodElement constructor = findConstructor(importedClass, arguments.size());
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
            MethodElement staticMethod = context.resolveStaticMethod(name, arguments.size());
            if (staticMethod != null) {
                return new Typed(
                    ClassTypeDef.of(staticMethod.getDeclaringType()).invokeStatic(staticMethod,
                        coercedArguments(staticMethod, arguments, ctx)),
                    staticMethod.getReturnType()
                );
            }
        }
        ExpressionDef target = runtime(EL_RESOLUTION, "resolveIdentifier", TypeDef.OBJECT,
            ctx, ExpressionDef.constant(name));
        return dynamic(runtime(EL_RESOLUTION, "invokeCallable", TypeDef.OBJECT,
            arguments(ctx, target, arguments)));
    }

    private Typed compileUnary(ELNode.Unary unary, ExpressionDef ctx) {
        ExpressionDef operand = compile(unary.operand(), ctx);
        return dynamic(switch (unary.operator()) {
            case NEGATE -> runtime(EL_ARITHMETIC, "negate", TypeDef.OBJECT, operand);
            case NOT -> runtime(EL_SUPPORT, "not", BOOLEAN, operand);
            case EMPTY -> runtime(EL_SUPPORT, "isEmpty", BOOLEAN, operand);
        });
    }

    private Typed compileBinary(ELNode.Binary binary, ExpressionDef ctx) {
        if (binary.operator() == BinaryOperator.AND) {
            return dynamic(new ExpressionDef.IfElse(
                toBoolean(compile(binary.left(), ctx)),
                toBoolean(compile(binary.right(), ctx)),
                ExpressionDef.constant(false)
            ));
        }
        if (binary.operator() == BinaryOperator.OR) {
            return dynamic(new ExpressionDef.IfElse(
                toBoolean(compile(binary.left(), ctx)),
                ExpressionDef.constant(true),
                toBoolean(compile(binary.right(), ctx))
            ));
        }
        ExpressionDef left = compile(binary.left(), ctx);
        ExpressionDef right = compile(binary.right(), ctx);
        return dynamic(switch (binary.operator()) {
            case ADD -> runtime(EL_ARITHMETIC, "add", TypeDef.OBJECT, left, right);
            case SUBTRACT -> runtime(EL_ARITHMETIC, "subtract", TypeDef.OBJECT, left, right);
            case MULTIPLY -> runtime(EL_ARITHMETIC, "multiply", TypeDef.OBJECT, left, right);
            case DIVIDE -> runtime(EL_ARITHMETIC, "divide", TypeDef.OBJECT, left, right);
            case MODULO -> runtime(EL_ARITHMETIC, "mod", TypeDef.OBJECT, left, right);
            case CONCAT -> runtime(EL_ARITHMETIC, "concat", STRING, left, right);
            case EQUAL -> runtime(EL_SUPPORT, "equals", BOOLEAN, left, right);
            case NOT_EQUAL -> runtime(EL_SUPPORT, "notEquals", BOOLEAN, left, right);
            case LESS_THAN -> runtime(EL_SUPPORT, "lessThan", BOOLEAN, left, right);
            case GREATER_THAN -> runtime(EL_SUPPORT, "greaterThan", BOOLEAN, left, right);
            case LESS_THAN_OR_EQUAL -> runtime(EL_SUPPORT, "lessThanOrEqual", BOOLEAN, left, right);
            case GREATER_THAN_OR_EQUAL -> runtime(EL_SUPPORT, "greaterThanOrEqual", BOOLEAN, left, right);
            case AND, OR -> throw new IllegalStateException("The logical operators are compiled separately");
        });
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

    private ExpressionDef compileLambda(ELNode.Lambda lambda, ExpressionDef ctx) {
        ClassElement bodyType = context.getVisitorContext().getClassElement(ELLambdaBody.class)
            .orElseThrow(() -> new ELCompilationException("Cannot resolve the type "
                + ELLambdaBody.class.getName() + ", the module is missing from the compilation classpath"));
        LambdaDef lambdaDef = ClassTypeDef.of(bodyType).getLambda();
        // the parameter of the body is renamed so that a nested lambda does not shadow the enclosing context
        String contextName = "elContext" + lambdas++;
        MethodDef implementation = MethodDef.builder(lambdaDef.getMethod().getName())
            .addModifiers(Modifier.PUBLIC)
            .addParameter(contextName, EL_CONTEXT)
            .returns(TypeDef.OBJECT)
            .build((aThis, parameters) -> {
                ExpressionDef lambdaContext = parameters.get(0);
                context.enterLambdaScope(lambda.parameters());
                try {
                    return compile(lambda.body(), lambdaContext).returning();
                } finally {
                    context.exitLambdaScope();
                }
            });
        ExpressionDef.Lambda body = new ExpressionDef.Lambda(
            lambdaDef.getType(), lambdaDef.getMethod(), implementation);
        List<ExpressionDef> names = lambda.parameters().stream()
            .map(parameter -> (ExpressionDef) ExpressionDef.constant(parameter))
            .toList();
        // the names are passed as an array: a generic List.of has a descriptor neither writer infers alike
        return runtime(EL_LAMBDAS, "lambda", LAMBDA_EXPRESSION,
            ctx, TypeDef.of(String.class).array().instantiate(names), body);
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
        ParameterElement[] parameters = method.getParameters();
        List<ExpressionDef> values = new ArrayList<>(arguments.size());
        for (int i = 0; i < arguments.size(); i++) {
            ExpressionDef value = compile(arguments.get(i), ctx);
            if (i < parameters.length) {
                values.add(coerce(value, parameters[i].getType(), ctx));
            } else {
                values.add(value);
            }
        }
        return values;
    }

    private ExpressionDef coerce(ExpressionDef value, ClassElement target, ExpressionDef ctx) {
        TypeDef targetType = TypeDef.erasure(target);
        // the section 1.25.8 of the specification coerces a lambda expression to a functional interface
        boolean functionalInterface = target.isInterface() && !target.isAssignable(LambdaExpression.class);
        return runtime(EL_SUPPORT, 
            functionalInterface ? "coerceToFunctionalInterface" : "coerceToType",
            targetType,
            ctx, value, ExpressionDef.constant(targetType)
        );
    }

    private ExpressionDef toBoolean(ExpressionDef value) {
        return runtime(EL_SUPPORT, "toBoolean", BOOLEAN, value);
    }

    private static boolean isStreamable(@Nullable ClassElement type) {
        return type != null && (type.isArray() || type.isAssignable(Collection.class));
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
            if (method.getParameters().length == arguments.size() && method.isPublic()
                && (!onlyStatic || ELTypes.isStatic(method))) {
                candidates.add(method);
            }
        }
        if (candidates.size() <= 1) {
            return candidates.isEmpty() ? null : candidates.get(0);
        }
        List<ClassElement> argumentTypes = arguments.stream().map(argument -> compileTyped(argument, ctx).type()).toList();
        MethodElement best = null;
        int bestScore = Integer.MAX_VALUE;
        boolean ambiguous = false;
        for (MethodElement candidate : candidates) {
            int score = score(candidate, argumentTypes);
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

    private static int score(MethodElement method, List<ClassElement> argumentTypes) {
        ParameterElement[] parameters = method.getParameters();
        int total = 0;
        for (int i = 0; i < parameters.length; i++) {
            ClassElement argument = argumentTypes.get(i);
            if (argument == null) {
                return -1;
            }
            ClassElement parameter = parameters[i].getType();
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
    private static MethodElement findConstructor(ClassElement type, int arguments) {
        for (MethodElement constructor : type.getEnclosedElements(ElementQuery.CONSTRUCTORS)) {
            if (constructor.isPublic() && constructor.getParameters().length == arguments) {
                return constructor;
            }
        }
        return null;
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
