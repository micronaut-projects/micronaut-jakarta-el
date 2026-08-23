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

import io.micronaut.core.annotation.Generated;
import io.micronaut.core.annotation.Internal;
import io.micronaut.el.annotation.ELFunction;
import io.micronaut.el.annotation.ELFunctionIndex;
import io.micronaut.el.processor.compiler.ELFunctionBinder;
import io.micronaut.el.processor.compiler.ELFunctionDiscovery;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.generator.SourceGenerator;
import io.micronaut.sourcegen.generator.SourceGenerators;
import io.micronaut.sourcegen.generator.bytecode.ByteCodeGenerator;
import io.micronaut.sourcegen.model.AnnotationDef;
import io.micronaut.sourcegen.model.ClassDef;

import javax.lang.model.element.Modifier;
import java.util.Set;

/**
 * Registers the functions declared with {@link ELFunction} for the expressions of the module, and indexes them
 * for the modules depending on it.
 *
 * <p>The visitors run in reverse order of {@link #getOrder()}, every class through one visitor before the next,
 * so this visitor sees every class of the round before {@link ELExpressionVisitor} compiles an expression: the
 * functions of the module are known whatever the order of its classes.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELFunctionIndexVisitor implements TypeElementVisitor<Object, Object> {

    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void start(VisitorContext visitorContext) {
        ELFunctionDiscovery.current().reset();
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of(ELFunction.class.getName());
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (!ELFunctionBinder.declaresFunctions(element)) {
            return;
        }
        ELFunctionDiscovery discovery = ELFunctionDiscovery.current();
        if (!discovery.register(element)) {
            return;
        }
        // no '$' in the name: the classpath scanner of Groovy skips such classes as inner classes
        String indexName = ELFunctionIndex.PACKAGE + ".Index_" + element.getName().replace('.', '_').replace('$', '_');
        ClassDef index = ClassDef.builder(indexName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(Generated.class)
            .addAnnotation(AnnotationDef.builder(ELFunctionIndex.class).addMember("value", element.getName()).build())
            .addJavadoc("The functions declared by <code>" + element.getName().replace("$", "15601") + "</code>.")
            .build();
        SourceGenerator generator = context.getLanguage() == VisitorContext.Language.JAVA
            ? SourceGenerators.findByLanguage(VisitorContext.Language.JAVA).orElseGet(ByteCodeGenerator::new)
            : new ByteCodeGenerator();
        generator.write(index, context, element);
    }
}
