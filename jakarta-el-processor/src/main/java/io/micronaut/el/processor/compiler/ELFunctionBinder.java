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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.el.annotation.ELFunction;
import io.micronaut.el.processor.visitor.ELTypes;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binds the functions a class declares to their qualified names.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELFunctionBinder {

    private ELFunctionBinder() {
    }

    /**
     * Binds the functions of a class: its public static methods, the public instance methods it declares, and
     * the {@code @JvmStatic} functions of its Kotlin companion object. Once a method of the class is annotated
     * with {@link ELFunction}, only the annotated methods are functions.
     *
     * @param type          The class
     * @param defaultPrefix The prefix of the functions that declare none
     * @param onlyAnnotated Whether only the methods annotated with {@link ELFunction} are bound
     * @param into          The functions, by qualified name
     */
    public static void bind(ClassElement type, String defaultPrefix, boolean onlyAnnotated, Map<String, MethodElement> into) {
        List<MethodElement> candidates = new ArrayList<>(type.getEnclosedElements(ElementQuery.ALL_METHODS.onlyAccessible()));
        for (ClassElement inner : type.getEnclosedElements(ElementQuery.ALL_INNER_CLASSES)) {
            if (ELTypes.isCompanion(inner)) {
                candidates.addAll(inner.getEnclosedElements(ElementQuery.ALL_METHODS.onlyAccessible()
                    .annotated(metadata -> metadata.hasAnnotation("kotlin.jvm.JvmStatic"))));
            }
        }
        boolean explicit = onlyAnnotated || candidates.stream().anyMatch(method -> method.hasAnnotation(ELFunction.class));
        for (MethodElement method : candidates) {
            if (!method.isPublic() || !(ELTypes.isStatic(method) || method.getDeclaringType().equals(type))) {
                continue;
            }
            AnnotationValue<ELFunction> function = method.getAnnotation(ELFunction.class);
            if (explicit && function == null) {
                continue;
            }
            String localName = method.getName();
            String prefix = defaultPrefix;
            if (function != null) {
                localName = function.stringValue().filter(value -> !value.isEmpty()).orElse(localName);
                prefix = function.stringValue("prefix").filter(value -> !value.isEmpty()).orElse(prefix);
            }
            into.putIfAbsent(CompilationContext.qualifiedFunctionName(prefix, localName), method);
        }
    }

    /**
     * @param type The class
     * @return Whether the class declares a function with {@link ELFunction}, on a method of its own or of its
     * Kotlin companion object
     */
    public static boolean declaresFunctions(ClassElement type) {
        if (!type.getEnclosedElements(ElementQuery.ALL_METHODS.onlyDeclared().annotated(m -> m.hasAnnotation(ELFunction.class))).isEmpty()) {
            return true;
        }
        for (ClassElement inner : type.getEnclosedElements(ElementQuery.ALL_INNER_CLASSES)) {
            if (ELTypes.isCompanion(inner)
                && !inner.getEnclosedElements(ElementQuery.ALL_METHODS.annotated(m -> m.hasAnnotation(ELFunction.class))).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
