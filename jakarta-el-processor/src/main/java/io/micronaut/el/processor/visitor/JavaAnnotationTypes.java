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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Recovers Java array class literals that the neutral annotation metadata cannot currently represent when
 * they are the value of a single {@code Class} member in a nested annotation.
 */
final class JavaAnnotationTypes {

    private JavaAnnotationTypes() {
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
