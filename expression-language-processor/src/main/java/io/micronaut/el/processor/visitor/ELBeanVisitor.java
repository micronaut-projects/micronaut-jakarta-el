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

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;
import io.micronaut.el.annotation.ELBean;
import io.micronaut.el.processor.writer.BeanResolverWriter;
import io.micronaut.el.resolver.ELBeanResolver;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.model.ClassDef;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The visitor generating the {@link ELBeanResolver} implementations of the types annotated with
 * {@link ELBean}.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELBeanVisitor implements TypeElementVisitor<ELBean, Object> {

    private static final String OBJECT = Object.class.getName();

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
        return Set.of(ELBean.class.getName());
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!processed.add(element.getName())) {
            return;
        }
        SourceGenerator sourceGenerator = SourceGenerators.findByLanguage(context.getLanguage()).orElse(null);
        if (sourceGenerator == null) {
            return;
        }
        try {
            AnnotationValue<ELBean> annotation = element.getAnnotation(ELBean.class);
            List<PropertyElement> properties = properties(element, annotation);
            Map<String, Map<Integer, MethodElement>> methods = annotation != null
                && !annotation.booleanValue("methods").orElse(true)
                ? Map.of()
                : methods(element);
            String className = element.getPackageName() + "." + element.getSimpleName() + "$ELResolver";
            ClassDef classDef = BeanResolverWriter.write(className, element, properties, methods);
            sourceGenerator.write(classDef, context, element);
            context.visitServiceDescriptor(ELBeanResolver.class.getName(), className, element);
        } catch (ProcessingException e) {
            processed.remove(element.getName());
            throw e;
        } catch (Exception e) {
            processed.remove(element.getName());
            SourceGenerators.handleFatalException(element, ELBean.class, e, exception -> {
                throw exception;
            });
        }
    }

    private static List<PropertyElement> properties(ClassElement element,
                                                    @Nullable AnnotationValue<ELBean> annotation) {
        Set<String> includes = annotation == null ? Set.of() : Set.of(annotation.stringValues("includes"));
        Set<String> excludes = annotation == null ? Set.of() : Set.of(annotation.stringValues("excludes"));
        List<PropertyElement> properties = new ArrayList<>();
        for (PropertyElement property : element.getBeanProperties()) {
            if (!includes.isEmpty() && !includes.contains(property.getName())) {
                continue;
            }
            if (excludes.contains(property.getName())) {
                continue;
            }
            if (property.getReadMethod().filter(MethodElement::isPublic).isEmpty()) {
                continue;
            }
            properties.add(property);
        }
        return properties;
    }

    private static Map<String, Map<Integer, MethodElement>> methods(ClassElement element) {
        Map<String, Map<Integer, MethodElement>> methods = new LinkedHashMap<>();
        for (MethodElement method : element.getEnclosedElements(
            ElementQuery.ALL_METHODS.onlyInstance().onlyAccessible())) {
            if (!method.isPublic() || OBJECT.equals(method.getDeclaringType().getName())) {
                continue;
            }
            methods.computeIfAbsent(method.getName(), name -> new LinkedHashMap<>())
                .putIfAbsent(method.getParameters().length, method);
        }
        return methods;
    }
}
