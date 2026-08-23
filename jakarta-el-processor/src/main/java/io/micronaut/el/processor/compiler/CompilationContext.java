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
import io.micronaut.el.processor.visitor.ELTypes;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.Element;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The compilation time counterpart of a {@code jakarta.el.ELContext}.
 *
 * <p>It holds the statically known types of the variables, the imported classes and packages, and the
 * functions of the expressions of a type, as declared with {@code io.micronaut.el.annotation.ELEnvironment}.
 * </p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class CompilationContext {

    private static final String DEFAULT_PACKAGE = "java.lang";

    private final VisitorContext visitorContext;
    private final Element originatingElement;
    private final Map<String, ClassElement> variables;
    private final Map<String, ClassElement> importedClasses;
    private final List<String> importedPackages;
    private final List<ClassElement> staticImports;
    private final Map<String, MethodElement> functions;
    private final Deque<List<String>> lambdaScopes = new ArrayDeque<>();
    private final Map<String, ClassElement> resolvedClasses = new HashMap<>();

    /**
     * @param visitorContext  The visitor context
     * @param originatingElement The element the expressions are declared on
     * @param variables       The types of the variables by name
     * @param importedClasses The imported classes by simple name
     * @param importedPackages The imported packages
     * @param staticImports   The classes imported statically
     * @param functions       The functions by qualified name
     */
    public CompilationContext(VisitorContext visitorContext,
                              Element originatingElement,
                              Map<String, ClassElement> variables,
                              Map<String, ClassElement> importedClasses,
                              List<String> importedPackages,
                              List<ClassElement> staticImports,
                              Map<String, MethodElement> functions) {
        this.visitorContext = visitorContext;
        this.originatingElement = originatingElement;
        this.variables = new LinkedHashMap<>(variables);
        this.importedClasses = new LinkedHashMap<>(importedClasses);
        this.importedPackages = new ArrayList<>(importedPackages);
        this.staticImports = List.copyOf(staticImports);
        this.functions = new LinkedHashMap<>(functions);
    }

    /**
     * @return The element the expressions are declared on, where the diagnostics are reported
     */
    public Element getOriginatingElement() {
        return originatingElement;
    }

    /**
     * @return The visitor context
     */
    public VisitorContext getVisitorContext() {
        return visitorContext;
    }

    /**
     * @param name The name of the variable
     * @return The declared type of the variable or {@code null} when the variable is not declared
     */
    @Nullable
    public ClassElement variableType(String name) {
        return isLambdaParameter(name) ? null : variables.get(name);
    }

    /**
     * @param name The name
     * @return True when the name is a parameter of one of the enclosing lambda expressions
     */
    public boolean isLambdaParameter(String name) {
        for (List<String> scope : lambdaScopes) {
            if (scope.contains(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enters the scope of a lambda expression.
     *
     * @param parameters The formal parameters
     */
    public void enterLambdaScope(List<String> parameters) {
        lambdaScopes.push(parameters);
    }

    /**
     * Exits the scope of a lambda expression.
     */
    public void exitLambdaScope() {
        lambdaScopes.pop();
    }

    /**
     * Resolves a class by its simple name, as described in the section 1.24.2 of the specification.
     *
     * @param simpleName The simple name
     * @return The class or {@code null} when the name is not an imported class
     */
    @Nullable
    public ClassElement resolveClass(String simpleName) {
        if (isLambdaParameter(simpleName) || variables.containsKey(simpleName)) {
            return null;
        }
        ClassElement imported = importedClasses.get(simpleName);
        if (imported != null) {
            return imported;
        }
        return resolvedClasses.computeIfAbsent(simpleName, this::resolveImportedPackageClass);
    }

    /**
     * Resolves a static field imported statically, as described in the section 1.24.2 of the specification.
     *
     * @param name The name of the field
     * @return The field or {@code null}
     */
    @Nullable
    public FieldElement resolveStaticField(String name) {
        for (ClassElement staticImport : staticImports) {
            Optional<FieldElement> field = staticImport
                .getEnclosedElements(ElementQuery.ALL_FIELDS.includeEnumConstants().named(name)).stream()
                .filter(ELTypes::isPublicStatic)
                .findFirst();
            if (field.isPresent()) {
                return field.get();
            }
        }
        return null;
    }

    /**
     * Resolves a static method imported statically, as described in the section 1.24.2 of the specification.
     *
     * @param name      The name of the method
     * @param arguments The number of arguments
     * @return The method or {@code null}
     */
    @Nullable
    public MethodElement resolveStaticMethod(String name, int arguments) {
        for (ClassElement staticImport : staticImports) {
            for (MethodElement method : staticImport
                .getEnclosedElements(ElementQuery.ALL_METHODS.onlyAccessible().named(name))) {
                if (method.getParameters().length == arguments && ELTypes.isStatic(method)) {
                    return method;
                }
            }
        }
        return null;
    }

    /**
     * @param prefix    The namespace prefix
     * @param localName The local name
     * @return The function or {@code null} when the function is not declared
     */
    @Nullable
    public MethodElement resolveFunction(String prefix, String localName) {
        return functions.get(qualifiedFunctionName(prefix, localName));
    }

    /**
     * @param prefix    The namespace prefix
     * @param localName The local name
     * @return The qualified name of a function
     */
    public static String qualifiedFunctionName(String prefix, String localName) {
        return prefix.isEmpty() ? localName : prefix + ":" + localName;
    }

    @Nullable
    private ClassElement resolveImportedPackageClass(String simpleName) {
        for (String importedPackage : importedPackages) {
            ClassElement element = visitorContext.getClassElement(importedPackage + "." + simpleName).orElse(null);
            if (element != null) {
                return element;
            }
        }
        return visitorContext.getClassElement(DEFAULT_PACKAGE + "." + simpleName).orElse(null);
    }
}
