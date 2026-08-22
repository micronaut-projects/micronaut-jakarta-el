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
package io.micronaut.el.processor.visitor;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.expressions.EvaluatedExpressionReference;
import io.micronaut.core.annotation.Internal;
import io.micronaut.el.ELExpressionSource;
import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELExpressions;
import io.micronaut.el.annotation.ELFunction;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELMethodExpressions;
import io.micronaut.el.annotation.ELVariable;
import io.micronaut.el.processor.compiler.CompilationContext;
import io.micronaut.el.processor.compiler.ELCompilationException;
import io.micronaut.el.processor.compiler.ELCompiler;
import io.micronaut.el.processor.compiler.ELExpressionDefinition;
import io.micronaut.el.processor.compiler.ELMethodExpressionDefinition;
import io.micronaut.el.parser.ELParser;
import io.micronaut.el.parser.ELParsingException;
import io.micronaut.el.processor.writer.ExpressionSourceWriter;
import io.micronaut.el.processor.writer.MethodExpressionWriter;
import io.micronaut.el.processor.writer.ValueExpressionWriter;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.bytecode.ByteCodeGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.model.ClassDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.Set;

/**
 * The visitor compiling the expressions declared with {@link ELExpression} and {@link ELMethodExpression}.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELExpressionVisitor implements TypeElementVisitor<Object, Object> {

    private final Set<String> processed = new HashSet<>();

    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public void start(VisitorContext visitorContext) {
        processed.clear();
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of(
            ELExpression.class.getName(),
            ELExpressions.class.getName(),
            ELMethodExpression.class.getName(),
            ELMethodExpressions.class.getName()
        );
    }

    /**
     * Reads the expression declared by an annotation.
     *
     * <p>Micronaut treats any annotation string containing {@code #{...}} as one of its own evaluated
     * expressions and replaces it with an {@link EvaluatedExpressionReference}, which would otherwise be
     * rendered as its {@code toString()}. The original text is taken back out of the reference.</p>
     *
     * @param annotation The annotation
     * @return The expression
     */
    private static Optional<String> expressionOf(AnnotationValue<?> annotation) {
        Object value = annotation.getValues().get(AnnotationMetadata.VALUE_MEMBER);
        if (value instanceof EvaluatedExpressionReference reference) {
            return Optional.of(reference.annotationValue().toString());
        }
        return annotation.stringValue();
    }

    /**
     * Selects how the generated classes are written.
     *
     * <p>A Java build gets readable Java sources. The source writers of the other languages cannot emit these
     * classes yet, so they get bytecode instead, which every language writes through
     * {@code VisitorContext.visitClass}.</p>
     *
     * @param context The context
     * @return The generator
     */
    private static SourceGenerator generatorFor(VisitorContext context) {
        if (context.getLanguage() == VisitorContext.Language.JAVA) {
            return SourceGenerators.findByLanguage(VisitorContext.Language.JAVA)
                .orElseGet(ByteCodeGenerator::new);
        }
        return new ByteCodeGenerator();
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!processed.add(element.getName())) {
            return;
        }
        List<AnnotationValue<ELExpression>> expressions = element.getAnnotationValuesByType(ELExpression.class);
        List<AnnotationValue<ELMethodExpression>> methodExpressions =
            element.getAnnotationValuesByType(ELMethodExpression.class);
        if (expressions.isEmpty() && methodExpressions.isEmpty()) {
            return;
        }
        SourceGenerator sourceGenerator = generatorFor(context);
        try {
            CompilationContext compilationContext = environmentOf(element, context);
            ELCompiler compiler = new ELCompiler(compilationContext);
            String prefix = element.getPackageName() + "." + element.getSimpleName();

            List<ExpressionSourceWriter.CompiledValue> compiledValues = new ArrayList<>(expressions.size());
            for (int i = 0; i < expressions.size(); i++) {
                ELExpressionDefinition definition = valueDefinition(expressions.get(i), i, context);
                String className = prefix + "$Expression" + i;
                ClassDef classDef = ValueExpressionWriter.write(className, definition, compiler);
                sourceGenerator.write(classDef, context, element);
                compiledValues.add(new ExpressionSourceWriter.CompiledValue(definition, className));
            }

            List<ExpressionSourceWriter.CompiledMethod> compiledMethods = new ArrayList<>(methodExpressions.size());
            for (int i = 0; i < methodExpressions.size(); i++) {
                ELMethodExpressionDefinition definition = methodDefinition(methodExpressions.get(i), i, context);
                String className = prefix + "$MethodExpression" + i;
                ClassDef classDef = MethodExpressionWriter.write(className, definition, compiler);
                sourceGenerator.write(classDef, context, element);
                compiledMethods.add(new ExpressionSourceWriter.CompiledMethod(definition, className));
            }

            String sourceClassName = prefix + "$ELExpressions";
            sourceGenerator.write(
                ExpressionSourceWriter.write(sourceClassName, compiledValues, compiledMethods), context, element);
            context.visitServiceDescriptor(ELExpressionSource.class.getName(), sourceClassName, element);
        } catch (ELParsingException | ELCompilationException | ProcessingException e) {
            processed.remove(element.getName());
            throw new ProcessingException(element, reportable(e), e);
        } catch (Exception e) {
            processed.remove(element.getName());
            if (e instanceof RuntimeException runtimeException
                && e.getClass().getSimpleName().equals("PostponeToNextRoundException")) {
                throw runtimeException;
            }
            throw new ProcessingException(element, "Failed to generate the expressions: " + reportable(e), e);
        }
    }

    /**
     * The annotation processor formats the messages it reports, and an expression may well contain a
     * {@code %}, as in {@code formatter.format('%1$.2f', value)}.
     */
    private static String reportable(Exception e) {
        return String.valueOf(e.getMessage()).replace("%", "%%");
    }

    private ELExpressionDefinition valueDefinition(AnnotationValue<ELExpression> annotation,
                                                   int index,
                                                   VisitorContext context) {
        String expression = expressionOf(annotation).orElseThrow(() ->
            new ELCompilationException("The expression of @ELExpression is required"));
        ClassElement expectedType = ELTypes.resolveMember(annotation, "expectedType", context)
            .orElseGet(() -> ClassElement.of(Object.class));
        String name = annotation.stringValue("name")
            .filter(value -> !value.isEmpty())
            .orElseGet(() -> "EXPRESSION_" + index);
        return new ELExpressionDefinition(expression, expectedType, constantName(name), ELParser.parse(expression));
    }

    private ELMethodExpressionDefinition methodDefinition(AnnotationValue<ELMethodExpression> annotation,
                                                          int index,
                                                          VisitorContext context) {
        String expression = expressionOf(annotation).orElseThrow(() ->
            new ELCompilationException("The expression of @ELMethodExpression is required"));
        ClassElement returnType = ELTypes.resolveMember(annotation, "expectedReturnType", context)
            .orElseGet(() -> ClassElement.of(Object.class));
        List<ClassElement> parameterTypes = ELTypes.resolveMembers(annotation, "expectedParamTypes", context);
        String name = annotation.stringValue("name")
            .filter(value -> !value.isEmpty())
            .orElseGet(() -> "METHOD_EXPRESSION_" + index);
        return new ELMethodExpressionDefinition(expression, returnType, parameterTypes, constantName(name),
            ELParser.parseEval(expression));
    }

    private CompilationContext environmentOf(ClassElement element, VisitorContext context) {
        Map<String, ClassElement> variables = new LinkedHashMap<>();
        Map<String, ClassElement> importedClasses = new LinkedHashMap<>();
        List<String> importedPackages = new ArrayList<>();
        List<ClassElement> staticImports = new ArrayList<>();
        Map<String, MethodElement> functions = new LinkedHashMap<>();

        AnnotationValue<ELEnvironment> environment = element.getAnnotation(ELEnvironment.class);
        if (environment != null) {
            for (AnnotationValue<ELVariable> variable : ELTypes.nested(environment, "variables", ELVariable.class)) {
                String name = variable.stringValue("name").orElseThrow(() ->
                    new ELCompilationException("The name of @ELVariable is required"));
                ClassElement type = ELTypes.resolveMember(variable, "type", context).orElseThrow(() ->
                    new ELCompilationException("The type of the variable '" + name + "' is required"));
                variables.put(name, type);
            }
            for (ClassElement imported : ELTypes.resolveMembers(environment, "imports", context)) {
                importedClasses.put(imported.getSimpleName(), imported);
            }
            importedPackages.addAll(List.of(environment.stringValues("importPackages")));
            staticImports.addAll(ELTypes.resolveMembers(environment, "staticImports", context));
            for (AnnotationValue<ELFunctions> declared : ELTypes.nested(environment, "functions", ELFunctions.class)) {
                registerFunctions(declared, context, functions);
            }
        }
        return new CompilationContext(context, variables, importedClasses, importedPackages, staticImports, functions);
    }

    private void registerFunctions(AnnotationValue<ELFunctions> declared,
                                   VisitorContext context,
                                   Map<String, MethodElement> functions) {
        ClassElement type = ELTypes.resolveMember(declared, "value", context).orElseThrow(() ->
            new ELCompilationException("The class of @ELFunctions is required"));
        String prefix = declared.stringValue("prefix").orElse("");
        for (MethodElement method : type.getEnclosedElements(ElementQuery.ALL_METHODS.onlyStatic().onlyAccessible())) {
            if (!method.isPublic()) {
                continue;
            }
            String localName = method.stringValue(ELFunction.class)
                .filter(value -> !value.isEmpty())
                .orElseGet(method::getName);
            functions.put(CompilationContext.qualifiedFunctionName(prefix, localName), method);
        }
    }

    private static String constantName(String name) {
        StringBuilder constant = new StringBuilder(name.length() + 4);
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isJavaIdentifierPart(c)) {
                appendSeparator(constant);
                continue;
            }
            if (Character.isUpperCase(c) && i > 0 && !Character.isUpperCase(name.charAt(i - 1))) {
                appendSeparator(constant);
            }
            constant.append(Character.toUpperCase(c));
        }
        String result = constant.toString();
        return result.isEmpty() || !Character.isJavaIdentifierStart(result.charAt(0)) ? "_" + result : result;
    }

    private static void appendSeparator(StringBuilder constant) {
        if (!constant.isEmpty() && constant.charAt(constant.length() - 1) != '_') {
            constant.append('_');
        }
    }
}
