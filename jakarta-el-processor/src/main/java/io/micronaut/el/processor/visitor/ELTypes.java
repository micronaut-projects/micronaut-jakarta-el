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

import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementModifier;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.FieldElement;
import io.micronaut.inject.ast.PrimitiveElement;
import io.micronaut.inject.visitor.VisitorContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Optional;

/**
 * The resolution of the types referenced by the annotations of the module.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELTypes {

    private ELTypes() {
    }

    /**
     * @param name    The name of the type
     * @param context The visitor context
     * @return The resolved type
     */
    static ClassElement resolve(String name, VisitorContext context) {
        return switch (name) {
            case "void" -> PrimitiveElement.VOID;
            case "boolean" -> PrimitiveElement.BOOLEAN;
            case "byte" -> PrimitiveElement.BYTE;
            case "short" -> PrimitiveElement.SHORT;
            case "int" -> PrimitiveElement.INT;
            case "long" -> PrimitiveElement.LONG;
            case "char" -> PrimitiveElement.CHAR;
            case "float" -> PrimitiveElement.FLOAT;
            case "double" -> PrimitiveElement.DOUBLE;
            default -> context.getClassElement(name)
                .orElseThrow(() -> new IllegalStateException("Cannot resolve the type " + name));
        };
    }

    /**
     * @param annotation The annotation
     * @param member     The member holding a class value
     * @param context    The visitor context
     * @return The resolved type
     */
    static Optional<ClassElement> resolveMember(AnnotationValue<?> annotation,
                                                String member,
                                                VisitorContext context) {
        return annotation.annotationClassValue(member)
            .map(AnnotationClassValue::getName)
            .map(name -> resolve(name, context));
    }

    /**
     * @param annotation The annotation
     * @param member     The member holding class values
     * @param context    The visitor context
     * @return The resolved types
     */
    static List<ClassElement> resolveMembers(AnnotationValue<?> annotation,
                                             String member,
                                             VisitorContext context) {
        List<ClassElement> types = new ArrayList<>();
        for (AnnotationClassValue<?> value : annotation.annotationClassValues(member)) {
            types.add(resolve(value.getName(), context));
        }
        return types;
    }

    /**
     * @param annotation The annotation
     * @param member     The member holding nested annotations
     * @param type       The type of the nested annotations
     * @param <T>        The type of the nested annotations
     * @return The nested annotations
     */
    static <T extends Annotation> List<AnnotationValue<T>> nested(AnnotationValue<?> annotation,
                                                                  String member,
                                                                  Class<T> type) {
        return annotation.getAnnotations(member, type);
    }

    /**
     * Whether the method is invoked statically. KSP reports the {@code @JvmStatic} functions of a Kotlin object
     * as instance methods, while the class has the static bridge the JVM invokes.
     *
     * @param method The method
     * @return True if the method is static on the JVM
     */
    public static boolean isStatic(MethodElement method) {
        return method.isStatic() || method.hasAnnotation("kotlin.jvm.JvmStatic");
    }

    /**
     * Whether the field is a public static field of the class. The visibility is read from the modifiers: KSP
     * reports a public field of a Java library class as private through {@code isPublic()} and
     * {@code isPrivate()}, while its modifiers are right.
     *
     * @param field The field
     * @return True if the field is readable statically
     */
    public static boolean isPublicStatic(FieldElement field) {
        Set<ElementModifier> modifiers = field.getModifiers();
        return modifiers.contains(ElementModifier.STATIC) && modifiers.contains(ElementModifier.PUBLIC);
    }
}
