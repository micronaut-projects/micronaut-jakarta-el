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

import io.micronaut.el.runtime.ELLambdaBody;
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import io.micronaut.sourcegen.model.VariableDef;
import org.jspecify.annotations.Nullable;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Compiles null-short-circuiting property and method continuations.
 */
final class ELAccessCompiler {

    private static final ClassTypeDef EL_RESOLUTION = ClassTypeDef.of(ELResolution.class);

    private final ELCompiler compiler;
    private final CompilationContext context;
    private final Map<String, ELCompiler.Typed> shared;
    private final List<MethodDef> accessMethods = new ArrayList<>();
    @Nullable
    private ClassTypeDef generatedType;
    private int locals;
    private int lambdas;

    ELAccessCompiler(ELCompiler compiler,
                     CompilationContext context,
                     Map<String, ELCompiler.Typed> shared) {
        this.compiler = compiler;
        this.context = context;
        this.shared = shared;
    }

    void beginClass(String className) {
        generatedType = ClassTypeDef.of(className);
        accessMethods.clear();
        locals = 0;
        lambdas = 0;
    }

    List<MethodDef> accessMethods() {
        return List.copyOf(accessMethods);
    }

    ELCompiler.Typed access(ELCompiler.Typed base,
                            ExpressionDef ctx,
                            boolean required,
                            BiFunction<ELCompiler.Typed, ExpressionDef, ELCompiler.Typed> continuation) {
        if (base.expression().type() instanceof TypeDef.Primitive || ELCompiler.isNonNull(base.expression())) {
            return continuation.apply(base, ctx);
        }
        if (generatedType != null) {
            // The continuation is an expression evaluated in place: a member of a stable base is guarded inline,
            // any other base is handed to a private helper, which evaluates it exactly once. Nothing is hoisted
            // ahead of the enclosing expression, so the operands of ?:, && and || that are not taken are not
            // evaluated, and the operands of every operator keep their left-to-right order.
            if (!required && stable(base.expression())) {
                ELCompiler.Typed inlined = inlineAccess(base, ctx, continuation);
                if (inlined != null) {
                    return inlined;
                }
            }
            return helperAccess(base, ctx, required, continuation);
        }
        return fallbackAccess(base, ctx, required, continuation);
    }

    /**
     * @return Whether private helper methods can be added to the class being compiled
     */
    boolean canAddHelpers() {
        return generatedType != null;
    }

    /**
     * Adds a private static helper to the class being compiled and returns its invocation with the operands: the
     * operands are evaluated once, in order, where the invocation stands, and the body sees them as parameters,
     * which it may read any number of times.
     *
     * @param prefix     The prefix of the name of the helper
     * @param operands   The operands
     * @param returnType The return type of the helper
     * @param body       Builds the body from the parameters
     * @return The invocation of the helper
     */
    ExpressionDef helper(String prefix,
                         List<ExpressionDef> operands,
                         TypeDef returnType,
                         java.util.function.Function<List<ExpressionDef>, StatementDef> body) {
        int index = locals++;
        MethodDef.MethodDefBuilder builder = MethodDef.builder(prefix + index)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .returns(returnType);
        for (int i = 0; i < operands.size(); i++) {
            builder.addParameter("operand" + index + "_" + i, helperParameterType(operands.get(i).type()));
        }
        MethodDef method = builder.build((aThis, parameters) -> body.apply(new ArrayList<>(parameters)));
        accessMethods.add(method);
        // the invocation refers to the signature alone: the bytecode writer looks for the variables a lambda
        // captures in the expressions nested in its body, which would otherwise reach into the statements of
        // the helper and take its parameters for variables of the enclosing method
        MethodDef signature = MethodDef.builder(method.getName())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameters(method.getParameters())
            .returns(returnType)
            .build();
        return Objects.requireNonNull(generatedType).invokeStatic(signature, operands);
    }

    private static TypeDef helperParameterType(TypeDef type) {
        if (type instanceof TypeDef.TypeVariable || type instanceof TypeDef.Wildcard || type.equals(TypeDef.VOID)) {
            return TypeDef.OBJECT;
        }
        return type;
    }

    private static boolean stable(ExpressionDef expression) {
        return expression instanceof VariableDef
            || (expression instanceof ExpressionDef.Cast cast && stable(cast.expressionDef()));
    }

    private ELCompiler.@Nullable Typed inlineAccess(
        ELCompiler.Typed base,
        ExpressionDef ctx,
        BiFunction<ELCompiler.Typed, ExpressionDef, ELCompiler.Typed> continuation) {
        ELCompiler.Typed result = continuation.apply(base, ctx);
        if (result.expression().type().equals(TypeDef.VOID)) {
            return null;
        }
        if (!(result.expression().type() instanceof TypeDef.Primitive primitive)) {
            ExpressionDef value = new ExpressionDef.IfElse(base.expression().isNull(),
                ExpressionDef.nullValue().cast(result.expression().type()), result.expression());
            return result.type() == null ? ELCompiler.dynamic(value) : new ELCompiler.Typed(value, result.type());
        }
        TypeDef wrapper = primitive.wrapperType();
        ExpressionDef nullable = new ExpressionDef.IfElse(base.expression().isNull(),
            ExpressionDef.nullValue().cast(wrapper), ELCompiler.boxed(result.expression()).cast(wrapper));
        ExpressionDef direct = new ExpressionDef.IfElse(base.expression().isNull(),
            primitiveDefault(primitive), result.expression());
        ClassElement type = result.type();
        return new ELCompiler.Typed(nullable,
            type == null ? null : ClassElement.of(ELCompiler.wrapperClass(type.getName())), direct, true);
    }

    private ELCompiler.Typed fallbackAccess(
        ELCompiler.Typed base,
        ExpressionDef ctx,
        boolean required,
        BiFunction<ELCompiler.Typed, ExpressionDef, ELCompiler.Typed> continuation) {
        ClassElement bodyType = compiler.elementOf(ELLambdaBody.Unary.class);
        MethodElement evaluate = Objects.requireNonNull(compiler.functionalMethod(bodyType));
        int index = lambdas++;
        MethodDef.MethodDefBuilder builder = MethodDef.builder(evaluate.getName())
            .addModifiers(Modifier.PUBLIC)
            .addParameter("elContext" + index, TypeDef.of(jakarta.el.ELContext.class))
            .addParameter("base" + index, TypeDef.OBJECT)
            .returns(TypeDef.OBJECT);
        ClassElement[] resultType = new ClassElement[1];
        MethodDef implementation = builder.build((aThis, parameters) -> {
            ExpressionDef accessContext = ELCompiler.captured(parameters.get(0));
            ExpressionDef parameter = ELCompiler.captured(parameters.get(1));
            ClassElement type = base.type();
            ELCompiler.Typed nonNullBase = type == null || type.isPrimitive() || ELCompiler.isUnknown(type)
                ? ELCompiler.dynamic(parameter)
                : new ELCompiler.Typed(parameter.cast(ELCompiler.erasure(type)), type);
            context.enterLambdaScope(Map.of(), true);
            try {
                ELCompiler.Typed result = continuation.apply(nonNullBase, accessContext);
                resultType[0] = result.type();
                if (result.type() != null && result.type().getName().equals("void")) {
                    return StatementDef.multi((StatementDef) result.expression(), ExpressionDef.nullValue().returning());
                }
                ExpressionDef value = ELCompiler.boxed(result.expression());
                return (required ? value
                    : new ExpressionDef.IfElse(parameter.isNull(), ExpressionDef.nullValue(), value)).returning();
            } finally {
                context.exitLambdaScope();
            }
        });
        ExpressionDef lambda = new ExpressionDef.Lambda(ClassTypeDef.of(bodyType), MethodDef.of(evaluate), implementation);
        ClassElement type = resultType[0];
        ExpressionDef result = compiler.runtime(EL_RESOLUTION, required ? "accessRequired" : "access",
            TypeDef.OBJECT, ctx, base.expression(), lambda);
        if (type == null || type.getName().equals("void")) {
            return ELCompiler.dynamic(result);
        }
        if (type.isPrimitive() && !type.isArray()) {
            type = ClassElement.of(ELCompiler.wrapperClass(type.getName()));
        }
        return new ELCompiler.Typed(result.cast(ELCompiler.erasure(type)), type);
    }

    private ELCompiler.Typed helperAccess(
        ELCompiler.Typed base,
        ExpressionDef ctx,
        boolean required,
        BiFunction<ELCompiler.Typed, ExpressionDef, ELCompiler.Typed> continuation) {
        Map<String, ELCompiler.Typed> captures = context.inValueLambda()
            ? new LinkedHashMap<>() : new LinkedHashMap<>(shared);
        captures.putAll(context.lambdaParameters());
        AccessHelper nullable = accessHelper(base, required, continuation, captures, null);
        ExpressionDef value = nullable.invocation(ctx, base.expression(), captures);
        ClassElement type = nullable.type();
        if (type == null || type.getName().equals("void")) {
            return ELCompiler.dynamic(value);
        }
        if (!type.isPrimitive()) {
            return new ELCompiler.Typed(value, type);
        }
        TypeDef.Primitive primitive = (TypeDef.Primitive) ELCompiler.erasure(type);
        AccessHelper direct = accessHelper(base, required, continuation, captures, primitive);
        return new ELCompiler.Typed(value, ClassElement.of(ELCompiler.wrapperClass(type.getName())),
            direct.invocation(ctx, base.expression(), captures), true);
    }

    // the access a compiled expression makes depends on what the model says about the base, the property
    // and the type each is used as, which is one decision rather than several
    @SuppressWarnings("java:S3776")
    private AccessHelper accessHelper(
        ELCompiler.Typed base,
        boolean required,
        BiFunction<ELCompiler.Typed, ExpressionDef, ELCompiler.Typed> continuation,
        Map<String, ELCompiler.Typed> captures,
        TypeDef.@Nullable Primitive primitiveReturn) {
        int index = locals++;
        MethodDef.MethodDefBuilder builder = MethodDef.builder("access" + index)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameter("elContext", TypeDef.of(jakarta.el.ELContext.class))
            .addParameter("base", base.expression().type());
        int captureIndex = 0;
        for (ELCompiler.Typed capture : captures.values()) {
            builder.addParameter("capture" + index + "_" + captureIndex++, capture.expression().type());
        }
        ClassElement[] resultType = new ClassElement[1];
        TypeDef[] returnType = new TypeDef[1];
        MethodDef method = builder.returns(primitiveReturn == null ? TypeDef.OBJECT : primitiveReturn)
            .build((aThis, methodParameters) -> {
                // the parameters are bound to locals: a Java lambda of the continuation captures them by name,
                // and the bytecode writer loads a captured variable as a local of the enclosing method
                List<StatementDef> bindings = new ArrayList<>(methodParameters.size());
                List<ExpressionDef> parameters = new ArrayList<>(methodParameters.size());
                for (VariableDef.MethodParameter methodParameter : methodParameters) {
                    VariableDef.Local local = new VariableDef.Local(methodParameter.name() + "Value", methodParameter.type());
                    bindings.add(new StatementDef.DefineAndAssign(local, methodParameter));
                    parameters.add(local);
                }
                ExpressionDef accessContext = parameters.get(0);
                ExpressionDef parameter = parameters.get(1);
                Map<String, ELCompiler.Typed> scope = new LinkedHashMap<>();
                int parameterIndex = 2;
                for (Map.Entry<String, ELCompiler.Typed> capture : captures.entrySet()) {
                    scope.put(capture.getKey(), new ELCompiler.Typed(parameters.get(parameterIndex++),
                        capture.getValue().type()));
                }
                ClassElement baseType = base.type();
                ELCompiler.Typed nonNullBase = baseType == null || baseType.isPrimitive()
                    || ELCompiler.isUnknown(baseType) ? ELCompiler.dynamic(parameter)
                    : new ELCompiler.Typed(parameter, baseType);
                context.enterLambdaScope(scope, true);
                try {
                    ELCompiler.Typed result = continuation.apply(nonNullBase, accessContext);
                    resultType[0] = result.type();
                    ExpressionDef expression = primitiveReturn == null ? result.expression()
                        : Objects.requireNonNull(result.primitiveExpression());
                    returnType[0] = primitiveReturn == null
                        ? expression.type() instanceof TypeDef.Primitive primitive ? primitive.wrapperType()
                        : expression.type().equals(TypeDef.VOID) ? TypeDef.OBJECT : expression.type()
                        : primitiveReturn;
                    StatementDef whenNull = (primitiveReturn == null
                        ? ExpressionDef.nullValue().cast(returnType[0]) : primitiveDefault(primitiveReturn)).returning();
                    StatementDef whenPresent = expression.type().equals(TypeDef.VOID)
                        ? StatementDef.multi((StatementDef) expression, ExpressionDef.nullValue().returning())
                        : (expression.type() instanceof TypeDef.Primitive && primitiveReturn == null
                            ? ELCompiler.boxed(expression).cast(returnType[0]) : expression).returning();
                    if (required) {
                        bindings.add((StatementDef) compiler.runtime(EL_RESOLUTION, "requireBase", TypeDef.VOID, parameter));
                        bindings.add(whenPresent);
                        return StatementDef.multi(bindings);
                    }
                    // Keep the continuation at method scope. Sourcegen 2.1.0 gives a scoped branch its own
                    // lambda-method collection and discards synthetic methods created inside that branch.
                    bindings.add(new StatementDef.If(parameter.isNull(), whenNull));
                    bindings.add(whenPresent);
                    return StatementDef.multi(bindings);
                } finally {
                    context.exitLambdaScope();
                }
            });
        MethodDef typedMethod = MethodDef.builder(method.getName())
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .addParameters(method.getParameters())
            .returns(returnType[0])
            .addStatements(method.getStatements())
            .build();
        accessMethods.add(typedMethod);
        return new AccessHelper(Objects.requireNonNull(generatedType), typedMethod, resultType[0]);
    }

    private static ExpressionDef primitiveDefault(TypeDef.Primitive primitive) {
        if (primitive.equals(TypeDef.Primitive.BOOLEAN)) {
            return ExpressionDef.constant(false);
        }
        if (primitive.equals(TypeDef.Primitive.CHAR)) {
            return ExpressionDef.constant('\0');
        }
        if (primitive.equals(TypeDef.Primitive.FLOAT)) {
            return ExpressionDef.constant(0.0f);
        }
        if (primitive.equals(TypeDef.Primitive.DOUBLE)) {
            return ExpressionDef.constant(0.0d);
        }
        return ExpressionDef.constant(0).cast(primitive);
    }

    private record AccessHelper(ClassTypeDef owner, MethodDef method, @Nullable ClassElement type) {

        private ExpressionDef invocation(ExpressionDef context,
                                         ExpressionDef base,
                                         Map<String, ELCompiler.Typed> captures) {
            List<ExpressionDef> arguments = new ArrayList<>(captures.size() + 2);
            arguments.add(context);
            arguments.add(base);
            captures.values().forEach(capture -> arguments.add(capture.expression()));
            return owner.invokeStatic(method, arguments);
        }
    }
}
