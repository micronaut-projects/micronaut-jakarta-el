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

import com.google.devtools.ksp.symbol.KSAnnotated;
import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSName;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSValueArgument;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.PrimitiveElement;
import io.micronaut.kotlin.processing.visitor.KotlinNativeElement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Recovers the primitive class literals of the annotations of a Kotlin source, which the Micronaut metadata turns
 * into their wrappers.
 *
 * <p>An annotation argument {@code Int::class} is {@code int.class} on the JVM, while {@code Integer::class}, the
 * only way to name the wrapper, is {@code java.lang.Integer}. The metadata builder of the Kotlin processor reads
 * the {@code KSType} of the argument and keeps only {@code KotlinVisitorContext.getBinaryName} of its declaration,
 * which maps {@code kotlin.Int} to {@code java.lang.Integer}: the two literals reach an {@code AnnotationValue} as
 * the same name. The KSP model of the element, which the native element of the Micronaut AST exposes, still holds
 * the {@code KSType} whose declaration is {@code kotlin.Int}, so the primitive is read from there.</p>
 *
 * <p>Loading this class loads types of KSP, which only a Kotlin compilation has on its classpath: the language is
 * checked before it is reached.</p>
 */
final class KotlinAnnotationTypes {

    private static final Map<String, PrimitiveElement> PRIMITIVES = Map.of(
        "kotlin.Boolean", PrimitiveElement.BOOLEAN,
        "kotlin.Byte", PrimitiveElement.BYTE,
        "kotlin.Short", PrimitiveElement.SHORT,
        "kotlin.Int", PrimitiveElement.INT,
        "kotlin.Long", PrimitiveElement.LONG,
        "kotlin.Char", PrimitiveElement.CHAR,
        "kotlin.Float", PrimitiveElement.FLOAT,
        "kotlin.Double", PrimitiveElement.DOUBLE
    );

    private KotlinAnnotationTypes() {
    }

    /**
     * The primitive a single {@code Class} member of a nested annotation names, such as the type of the
     * {@code @ELVariable} at an index of {@code @ELEnvironment(variables = ...)}.
     *
     * @param nativeType      The native element carrying the annotation
     * @param annotationName  The name of the annotation
     * @param containerMember The member holding the nested annotations
     * @param nestedIndex     The index of the nested annotation
     * @param nestedMember    The {@code Class} member of the nested annotation
     * @return The primitive, empty when the literal is not a Kotlin primitive or cannot be read
     */
    static Optional<ClassElement> resolveNestedPrimitive(Object nativeType,
                                                         String annotationName,
                                                         String containerMember,
                                                         int nestedIndex,
                                                         String nestedMember) {
        for (KSAnnotation annotation : annotations(nativeType)) {
            if (!annotationName.equals(annotationName(annotation))) {
                continue;
            }
            if (!(argument(annotation, containerMember) instanceof List<?> nested) || nestedIndex >= nested.size()
                || !(nested.get(nestedIndex) instanceof KSAnnotation declaration)) {
                return Optional.empty();
            }
            return Optional.ofNullable(primitive(argument(declaration, nestedMember)));
        }
        return Optional.empty();
    }

    /**
     * The types of a {@code Class[]} member of the declarations of a repeatable annotation, the metadata types
     * with the primitives the Kotlin source names put back in place of their wrappers.
     *
     * @param nativeType           The native element carrying the annotation
     * @param annotationName       The name of the annotation
     * @param discriminatorMembers The members telling the declarations apart
     * @param discriminatorValue   The value identifying the declaration
     * @param nameMember           The member naming the declaration
     * @param nameValue            The name identifying it, empty when it declares none
     * @param member               The {@code Class[]} member to read
     * @param recovered            The types the metadata kept, a primitive as its wrapper
     * @return The types of every matching declaration
     */
    static List<List<ClassElement>> resolveRepeatableMemberTypes(Object nativeType,
                                                                 String annotationName,
                                                                 List<String> discriminatorMembers,
                                                                 String discriminatorValue,
                                                                 String nameMember,
                                                                 String nameValue,
                                                                 String member,
                                                                 List<ClassElement> recovered) {
        List<List<ClassElement>> matches = new ArrayList<>(1);
        for (KSAnnotation annotation : annotations(nativeType)) {
            for (KSAnnotation declaration : declarations(annotation, annotationName)) {
                if (isDeclaration(declaration, discriminatorMembers, discriminatorValue, nameMember, nameValue)
                    && argument(declaration, member) instanceof List<?> values
                    && values.size() == recovered.size()) {
                    matches.add(memberTypes(values, recovered));
                }
            }
        }
        return matches;
    }

    /**
     * Whether the declaration is the one identified by the discriminator and, when given, the name.
     */
    private static boolean isDeclaration(KSAnnotation declaration,
                                         List<String> discriminatorMembers,
                                         String discriminatorValue,
                                         String nameMember,
                                         String nameValue) {
        if (discriminatorMembers.stream().noneMatch(name ->
            Objects.equals(discriminatorValue, argument(declaration, name)))) {
            return false;
        }
        return nameValue.isEmpty() || Objects.equals(nameValue, argument(declaration, nameMember));
    }

    /**
     * The types of the class literals of a member, a primitive put back in place of the wrapper the metadata kept.
     */
    private static List<ClassElement> memberTypes(List<?> values, List<ClassElement> recovered) {
        List<ClassElement> types = new ArrayList<>(values.size());
        for (int i = 0; i < values.size(); i++) {
            PrimitiveElement primitive = primitive(values.get(i));
            types.add(primitive != null ? primitive : recovered.get(i));
        }
        return types;
    }

    /**
     * The primitive a class literal names: its declaration is one of the primitive classes of Kotlin, which a
     * class literal cannot name nullable, so it is the primitive on the JVM.
     */
    @Nullable
    private static PrimitiveElement primitive(@Nullable Object value) {
        if (!(value instanceof KSType type) || type.isMarkedNullable() || type.isError()) {
            return null;
        }
        KSName name = type.getDeclaration().getQualifiedName();
        return name == null ? null : PRIMITIVES.get(name.asString());
    }

    private static List<KSAnnotation> annotations(Object nativeType) {
        if (!(nativeType instanceof KotlinNativeElement kotlinElement)) {
            return List.of();
        }
        KSAnnotated element = kotlinElement.getElement();
        List<KSAnnotation> annotations = new ArrayList<>();
        for (Iterator<KSAnnotation> iterator = element.getAnnotations().iterator(); iterator.hasNext(); ) {
            annotations.add(iterator.next());
        }
        return annotations;
    }

    /**
     * The declarations of the annotation, which is either the annotation itself or the container a repeated one
     * may be wrapped in.
     */
    private static List<KSAnnotation> declarations(KSAnnotation annotation, String annotationName) {
        if (annotationName.equals(annotationName(annotation))) {
            return List.of(annotation);
        }
        if (!(argument(annotation, "value") instanceof List<?> values)) {
            return List.of();
        }
        List<KSAnnotation> declarations = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value instanceof KSAnnotation declaration && annotationName.equals(annotationName(declaration))) {
                declarations.add(declaration);
            }
        }
        return declarations;
    }

    @Nullable
    private static Object argument(KSAnnotation annotation, String member) {
        for (KSValueArgument argument : annotation.getArguments()) {
            KSName name = argument.getName();
            if (name != null && name.asString().equals(member)) {
                return argument.getValue();
            }
        }
        return null;
    }

    private static String annotationName(KSAnnotation annotation) {
        KSDeclaration declaration = annotation.getAnnotationType().resolve().getDeclaration();
        KSName name = declaration.getQualifiedName();
        return name == null ? "" : name.asString();
    }
}
