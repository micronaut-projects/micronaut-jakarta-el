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

import io.micronaut.annotation.processing.visitor.JavaNativeElement;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.VisitorContext;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.Optional;

/**
 * Recovers Java class literals the neutral annotation metadata cannot currently represent: an array literal
 * that is the value of a single {@code Class} member of a nested annotation, and the primitive literals of a
 * {@code Class[]} member, which the metadata drops entirely.
 *
 * <p>Both read the {@code javax.lang.model} mirrors of the element, so they recover nothing when the class is
 * compiled by the Groovy or the Kotlin processor: a primitive declared there is still dropped, and the
 * annotation has to name the wrapper type instead.</p>
 */
final class JavaAnnotationTypes {

    private JavaAnnotationTypes() {
    }

    static boolean isFunctionalInterfaceCandidate(Object nativeType) {
        return !(nativeType instanceof JavaNativeElement javaElement)
            || javaElement.element() == null
            || (javaElement.element().getKind() != ElementKind.ANNOTATION_TYPE
                && !javaElement.element().getModifiers().contains(Modifier.SEALED));
    }

    static Optional<ClassElement> resolveNestedMember(Object nativeType,
                                                      String annotationName,
                                                      String containerMember,
                                                      int nestedIndex,
                                                      String nestedMember,
                                                      VisitorContext context) {
        if (!(nativeType instanceof JavaNativeElement javaElement) || javaElement.element() == null) {
            return Optional.empty();
        }
        for (AnnotationMirror annotation : javaElement.element().getAnnotationMirrors()) {
            if (!annotationName.equals(JavaAnnotationTypes.annotationName(annotation))) {
                continue;
            }
            Object containers = memberValue(annotation, containerMember).orElse(null);
            if (!(containers instanceof List<?> values) || nestedIndex >= values.size()) {
                return Optional.empty();
            }
            Object nested = values.get(nestedIndex);
            if (nested instanceof javax.lang.model.element.AnnotationValue value) {
                nested = value.getValue();
            }
            if (!(nested instanceof AnnotationMirror nestedAnnotation)) {
                return Optional.empty();
            }
            Object type = memberValue(nestedAnnotation, nestedMember).orElse(null);
            return type instanceof TypeMirror mirror ? resolve(mirror, context) : Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * The class literals of a {@code Class[]} member of one declaration of a repeatable annotation, which the
     * neutral metadata drops when they are primitive.
     *
     * @param nativeType          The native element carrying the annotation
     * @param annotationName      The name of the annotation
     * @param discriminatorMembers The members telling the declarations apart, an aliased one being
     *                             recorded by the compiler under the name that was written
     * @param discriminatorValue  The value identifying the declaration
     * @param nameMember          The member naming the declaration
     * @param nameValue           The name identifying it, empty when it declares none
     * @param member              The {@code Class[]} member to read
     * @param context             The visitor context
     * @return The types every matching declaration names, since a repeatable annotation can carry the same
     * discriminator more than once
     */
    static List<List<ClassElement>> resolveRepeatableMemberTypes(Object nativeType,
                                                                 String annotationName,
                                                                 List<String> discriminatorMembers,
                                                                 String discriminatorValue,
                                                                 String nameMember,
                                                                 String nameValue,
                                                                 String member,
                                                                 VisitorContext context) {
        if (!(nativeType instanceof JavaNativeElement javaElement) || javaElement.element() == null) {
            return List.of();
        }
        List<List<ClassElement>> matches = new ArrayList<>(1);
        for (AnnotationMirror annotation : javaElement.element().getAnnotationMirrors()) {
            for (AnnotationMirror declaration : declarations(annotation, annotationName)) {
                if (discriminatorMembers.stream().noneMatch(name ->
                    Objects.equals(discriminatorValue, memberValue(declaration, name).orElse(null)))) {
                    continue;
                }
                // the name a declaration carries tells it apart from another of the same expression, which
                // the parameter types cannot when the metadata dropped every one of them
                if (!nameValue.isEmpty()
                    && !Objects.equals(nameValue, memberValue(declaration, nameMember).orElse(null))) {
                    continue;
                }
                if (!(memberValue(declaration, member).orElse(null) instanceof List<?> values)) {
                    continue;
                }
                List<ClassElement> types = new ArrayList<>(values.size());
                for (Object value : values) {
                    if (unwrap(value) instanceof TypeMirror mirror) {
                        resolve(mirror, context).ifPresent(types::add);
                    }
                }
                matches.add(types);
            }
        }
        return matches;
    }

    /**
     * The declarations of the annotation carried by the mirror, which is either the annotation itself or the
     * container the compiler wraps a repeated one in.
     */
    private static List<AnnotationMirror> declarations(AnnotationMirror annotation, String annotationName) {
        if (annotationName.equals(annotationName(annotation))) {
            return List.of(annotation);
        }
        if (!(memberValue(annotation, "value").orElse(null) instanceof List<?> values)) {
            return List.of();
        }
        List<AnnotationMirror> declarations = new ArrayList<>(values.size());
        for (Object value : values) {
            if (unwrap(value) instanceof AnnotationMirror declaration
                && annotationName.equals(annotationName(declaration))) {
                declarations.add(declaration);
            }
        }
        return declarations;
    }

    private static Object unwrap(Object value) {
        return value instanceof javax.lang.model.element.AnnotationValue annotationValue
            ? annotationValue.getValue() : value;
    }

    private static Optional<Object> memberValue(AnnotationMirror annotation, String member) {
        for (Map.Entry<? extends ExecutableElement, ? extends javax.lang.model.element.AnnotationValue> entry
            : annotation.getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().contentEquals(member)) {
                return Optional.ofNullable(entry.getValue().getValue());
            }
        }
        return Optional.empty();
    }

    private static String annotationName(AnnotationMirror annotation) {
        return annotation.getAnnotationType().asElement() instanceof TypeElement type
            ? type.getQualifiedName().toString() : "";
    }

    private static Optional<ClassElement> resolve(TypeMirror mirror, VisitorContext context) {
        if (mirror instanceof ArrayType array) {
            return resolve(array.getComponentType(), context).map(ClassElement::toArray);
        }
        if (mirror instanceof DeclaredType declared && declared.asElement() instanceof TypeElement type) {
            return context.getClassElement(type.getQualifiedName().toString());
        }
        return Optional.of(ELTypes.resolve(mirror.toString(), context));
    }
}
