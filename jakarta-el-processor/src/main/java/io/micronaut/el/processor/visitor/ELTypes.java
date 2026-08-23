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
import io.micronaut.inject.ast.PrimitiveElement;
import io.micronaut.inject.visitor.VisitorContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The resolution of the types referenced by the annotations of the module.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class ELTypes {

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
}
