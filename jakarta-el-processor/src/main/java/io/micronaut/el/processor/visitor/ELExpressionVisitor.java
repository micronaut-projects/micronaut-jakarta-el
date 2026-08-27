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

import java.io.IOException;
import java.io.Writer;

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.expressions.EvaluatedExpressionReference;
import io.micronaut.core.annotation.Internal;
import io.micronaut.el.ELExpressionSource;
import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELExpressions;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELMethodExpressions;
import io.micronaut.el.annotation.ELVariable;
import io.micronaut.el.processor.compiler.CompilationContext;
import io.micronaut.el.processor.compiler.ELCompilationException;
import io.micronaut.el.processor.compiler.ELCompiler;
import io.micronaut.el.processor.compiler.ELFunctionDiscovery;
import io.micronaut.el.processor.compiler.ELUndeclaredFunctionException;
import io.micronaut.el.processor.compiler.ELFunctionBinder;
import io.micronaut.el.processor.compiler.ELExpressionDefinition;
import io.micronaut.el.processor.compiler.ELMethodExpressionDefinition;
import io.micronaut.el.parser.ELParser;
import io.micronaut.el.parser.ELParsingException;
import io.micronaut.el.processor.writer.ExpressionSourceWriter;
import io.micronaut.el.processor.writer.MethodExpressionWriter;
import io.micronaut.el.processor.writer.ValueExpressionWriter;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.bytecode.ByteCodeGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.model.ClassDef;

import java.lang.annotation.Annotation;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final List<ClassElement> pending = new ArrayList<>();

    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public void start(VisitorContext visitorContext) {
        processed.clear();
        pending.clear();
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
        try {
            compile(element, context);
        } catch (ProcessingException e) {
            if (e.getCause() instanceof ELUndeclaredFunctionException && context.getLanguage() != VisitorContext.Language.KOTLIN) {
                // the function may be declared in a class of the module not visited yet: Groovy visits the
                // classes one source file at a time; the class is retried once every class is visited (KSP
                // does not let an element be used after the round, and visits every class first anyway)
                processed.remove(element.getName());
                pending.add(element);
                return;
            }
            // reported at the declaring class, as it is: the processors of the languages would format it
            context.fail(String.valueOf(e.getMessage()), element);
        }
    }

    @Override
    public void finish(VisitorContext context) {
        List<ClassElement> retried = new ArrayList<>(pending);
        pending.clear();
        for (ClassElement element : retried) {
            if (!processed.add(element.getName())) {
                continue;
            }
            try {
                compile(element, context);
            } catch (ProcessingException e) {
                context.fail(String.valueOf(e.getMessage()), element);
            }
        }
    }

    private void compile(ClassElement element, VisitorContext context) {
        List<Declared<ELExpression>> expressions = declaredOn(element, ELExpression.class);
        List<Declared<ELMethodExpression>> methodExpressions = declaredOn(element, ELMethodExpression.class);
        if (expressions.isEmpty() && methodExpressions.isEmpty()) {
            return;
        }
        SourceGenerator sourceGenerator = generatorFor(context);
        try {
            Map<Element, ELCompiler> compilers = new LinkedHashMap<>();
            String prefix = element.getPackageName() + "." + element.getSimpleName();
            // every expression is compiled before anything is written: a class retried later must not find
            // its first classes generated already
            List<ClassDef> generated = new ArrayList<>();

            List<ExpressionSourceWriter.CompiledValue> compiledValues = new ArrayList<>(expressions.size());
            Set<String> constantNames = new HashSet<>();
            for (int i = 0; i < expressions.size(); i++) {
                Declared<ELExpression> declared = expressions.get(i);
                ELExpressionDefinition definition = valueDefinition(declared.annotation(), declared.owner(), constantNames, context);
                String className = prefix + "$Expression" + i;
                ValueExpressionWriter.Written written = ValueExpressionWriter.write(className, definition,
                    compilerFor(declared.owner(), element, compilers, context));
                generated.add(written.type());
                compiledValues.add(new ExpressionSourceWriter.CompiledValue(written.definition(), className));
            }

            List<ExpressionSourceWriter.CompiledMethod> compiledMethods = new ArrayList<>(methodExpressions.size());
            for (int i = 0; i < methodExpressions.size(); i++) {
                Declared<ELMethodExpression> declared = methodExpressions.get(i);
                ELMethodExpressionDefinition definition = methodDefinition(declared.annotation(), declared.owner(), constantNames, context);
                String className = prefix + "$MethodExpression" + i;
                MethodExpressionWriter.Written written = MethodExpressionWriter.write(className, definition,
                    compilerFor(declared.owner(), element, compilers, context));
                generated.add(written.type());
                compiledMethods.add(new ExpressionSourceWriter.CompiledMethod(written.definition(), className));
            }

            String sourceClassName = prefix + "$ELExpressions";
            generated.add(ExpressionSourceWriter.write(sourceClassName, compiledValues, compiledMethods));
            for (ClassDef classDef : generated) {
                sourceGenerator.write(classDef, context, element);
            }
            context.visitServiceDescriptor(ELExpressionSource.class.getName(), sourceClassName, element);
            visitNativeImageProperties(sourceClassName, generated, element, context);
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
     * Marks the generated classes for build-time initialization in a GraalVM native image.
     *
     * <p>The classes are immutable singletons created in their static initializers. GraalVM simulates such
     * initializers and folds the constants of the registry into the code that reads them, so the singletons
     * end up in the image heap - which the image builder only allows for classes explicitly declared safe to
     * initialize at build time. The properties file makes that declaration, and keeps a native image build
     * configuration-free.</p>
     */
    private static void visitNativeImageProperties(String sourceClassName, List<ClassDef> generated, ClassElement element, VisitorContext context) {
        String classes = generated.stream()
            .map(ClassDef::getName)
            .collect(java.util.stream.Collectors.joining(","));
        context.visitMetaInfFile("native-image/io.micronaut.el.generated/" + sourceClassName + "/native-image.properties", element)
            .ifPresent(file -> {
                try (Writer writer = file.openWriter()) {
                    writer.write("Args = --initialize-at-build-time=" + classes + "\n");
                } catch (IOException e) {
                    throw new ProcessingException(element, "Failed to write the native-image properties: " + e.getMessage(), e);
                }
            });
    }

    /**
     * The message of a failure, reported through {@link VisitorContext#fail} which prints it as it is: an
     * expression may well contain a {@code %}, as in {@code formatter.format('%1$.2f', value)}, and the
     * processors of the languages would format it otherwise.
     */
    private static String reportable(Exception e) {
        return String.valueOf(e.getMessage());
    }

    /**
     * The declarations of an annotation on the class and on its declared fields, methods and parameters, in
     * that order. An expression declared twice with the same types is compiled once.
     */
    private static <A extends Annotation> List<Declared<A>> declaredOn(ClassElement element, Class<A> annotation) {
        List<Declared<A>> declared = new ArrayList<>();
        collect(element, annotation, declared);
        for (FieldElement field : element.getEnclosedElements(ElementQuery.ALL_FIELDS.onlyDeclared())) {
            collect(field, annotation, declared);
        }
        for (MethodElement method : element.getEnclosedElements(ElementQuery.ALL_METHODS.onlyDeclared())) {
            collect(method, annotation, declared);
            for (ParameterElement parameter : method.getParameters()) {
                collect(parameter, annotation, declared);
            }
        }
        Map<String, Declared<A>> distinct = new LinkedHashMap<>();
        for (Declared<A> value : declared) {
            String key = expressionOf(value.annotation()).orElse("") + "|"
                + value.annotation().annotationClassValue("expectedType").map(AnnotationClassValue::getName).orElse("")
                + "|" + value.annotation().annotationClassValue("expectedReturnType").map(AnnotationClassValue::getName).orElse("")
                + "|" + Arrays.stream(value.annotation().annotationClassValues("expectedParamTypes"))
                    .map(AnnotationClassValue::getName)
                    .collect(java.util.stream.Collectors.joining(","));
            distinct.putIfAbsent(key, value);
        }
        return new ArrayList<>(distinct.values());
    }

    private static <A extends Annotation> void collect(Element owner, Class<A> annotation, List<Declared<A>> into) {
        for (AnnotationValue<A> value : owner.getAnnotationValuesByType(annotation)) {
            into.add(new Declared<>(value, owner));
        }
    }

    /**
     * The compiler of the expressions declared on an element: the environment of the class, the environment of
     * the element, and, for a method or one of its parameters, the parameters of the method as variables.
     */
    private ELCompiler compilerFor(Element owner, ClassElement element, Map<Element, ELCompiler> compilers, VisitorContext context) {
        return compilers.computeIfAbsent(owner, key -> new ELCompiler(environmentOf(element, owner, context)));
    }

    private ELExpressionDefinition valueDefinition(AnnotationValue<ELExpression> annotation,
                                                   Element owner,
                                                   Set<String> used,
                                                   VisitorContext context) {
        String expression = expressionOf(annotation).orElseThrow(() ->
            new ELCompilationException("The expression of @ELExpression is required"));
        // an omitted type is inferred from the static type of the expression once it is compiled
        ClassElement expectedType = ELTypes.resolveMember(annotation, "expectedType", context).orElse(null);
        String name = uniqueConstantName(annotation, owner, expression, used);
        return new ELExpressionDefinition(expression, expectedType, false, name, ELParser.parse(expression));
    }

    private ELMethodExpressionDefinition methodDefinition(AnnotationValue<ELMethodExpression> annotation,
                                                          Element owner,
                                                          Set<String> used,
                                                          VisitorContext context) {
        String expression = expressionOf(annotation).orElseThrow(() ->
            new ELCompilationException("The expression of @ELMethodExpression is required"));
        ClassElement returnType = ELTypes.resolveMember(annotation, "expectedReturnType", context).orElse(null);
        List<ClassElement> parameterTypes = ELTypes.resolveMembers(annotation, "expectedParamTypes", context);
        String name = uniqueConstantName(annotation, owner, expression, used);
        return new ELMethodExpressionDefinition(expression, returnType, false, parameterTypes, name,
            ELParser.parseEval(expression));
    }

    private CompilationContext environmentOf(ClassElement element, Element owner, VisitorContext context) {
        Map<String, ClassElement> variables = new LinkedHashMap<>();
        Map<String, ClassElement> importedClasses = new LinkedHashMap<>();
        List<String> importedPackages = new ArrayList<>();
        List<ClassElement> staticImports = new ArrayList<>();
        Map<String, MethodElement> functions = new LinkedHashMap<>();

        MethodElement method = owner instanceof MethodElement methodElement ? methodElement
            : owner instanceof ParameterElement parameter ? parameter.getMethodElement() : null;
        if (method != null) {
            for (ParameterElement parameter : method.getParameters()) {
                variables.put(parameter.getName(), parameter.getGenericType());
            }
        }
        declare(element.getAnnotation(ELEnvironment.class), element, context, variables, importedClasses, importedPackages, staticImports, functions);
        if (owner != element) {
            declare(owner.getAnnotation(ELEnvironment.class), owner, context, variables, importedClasses, importedPackages, staticImports, functions);
        }
        return new CompilationContext(context, owner, ELFunctionDiscovery.current(), variables, importedClasses, importedPackages, staticImports, functions);
    }

    private void declare(@Nullable AnnotationValue<ELEnvironment> environment,
                         Element owner,
                         VisitorContext context,
                         Map<String, ClassElement> variables,
                         Map<String, ClassElement> importedClasses,
                         List<String> importedPackages,
                         List<ClassElement> staticImports,
                         Map<String, MethodElement> functions) {
        if (environment == null) {
            return;
        }
        List<AnnotationValue<ELVariable>> declaredVariables = ELTypes.nested(environment, "variables", ELVariable.class);
        for (int i = 0; i < declaredVariables.size(); i++) {
            int variableIndex = i;
            AnnotationValue<ELVariable> variable = declaredVariables.get(i);
            String name = variable.stringValue("name").orElseThrow(() ->
                new ELCompilationException("The name of @ELVariable is required"));
            ClassElement type = ELTypes.resolveMember(variable, "type", context)
                .or(() -> context.getLanguage() == VisitorContext.Language.JAVA
                    ? JavaAnnotationTypes.resolveNestedMember(owner.getNativeType(), ELEnvironment.class.getName(),
                        "variables", variableIndex, "type", context)
                    : Optional.empty())
                .orElseThrow(() ->
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

    private void registerFunctions(AnnotationValue<ELFunctions> declared,
                                   VisitorContext context,
                                   Map<String, MethodElement> functions) {
        // the alias is resolved by the metadata builder for the annotations of the sources, not for the ones a
        // remapper builds: both members are read
        ClassElement type = ELTypes.resolveMember(declared, "value", context)
            .or(() -> ELTypes.resolveMember(declared, "type", context))
            .filter(resolved -> !resolved.getName().equals("void"))
            .orElseThrow(() -> new ELCompilationException("The class of @ELFunctions is required"));
        ELFunctionBinder.bind(type, declared.stringValue("prefix").orElse(""), false, functions);
    }

    /**
     * The name of the constant holding a compiled expression in the generated registry. The declared
     * {@code name} wins; without one the name is derived from the declaration: the name of the method, field
     * or parameter the expression is declared on, or, for an expression declared on the class itself, from the
     * text of the expression. A name a previous expression of the class already took gets a numeric suffix.
     */
    private static String uniqueConstantName(AnnotationValue<?> annotation, Element owner, String expression, Set<String> used) {
        String base = annotation.stringValue("name")
            .filter(value -> !value.isEmpty())
            .map(ELExpressionVisitor::constantName)
            .orElseGet(() -> owner instanceof ClassElement
                ? derivedFromExpression(expression)
                : constantName(owner.getName()));
        String name = base;
        for (int i = 2; !used.add(name); i++) {
            name = base + "_" + i;
        }
        return name;
    }

    private static String derivedFromExpression(String expression) {
        String text = expression.replace("${", " ").replace("#{", " ").replace("}", " ").trim();
        String derived = constantName(text);
        if (derived.length() > 44) {
            int cut = derived.lastIndexOf('_', 44);
            derived = derived.substring(0, cut > 20 ? cut : 44);
        }
        return derived;
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

    /**
     * A declaration of an expression and the element it is declared on, whose environment compiles it.
     *
     * @param annotation The declaration
     * @param owner      The element it is declared on
     * @param <A>        The annotation type
     */
    private record Declared<A extends Annotation>(AnnotationValue<A> annotation, Element owner) {
    }
}
