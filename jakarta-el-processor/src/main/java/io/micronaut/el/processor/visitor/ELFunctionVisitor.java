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

import io.micronaut.core.annotation.Internal;
import io.micronaut.el.annotation.ELFunction;
import io.micronaut.el.processor.compiler.ELFunctionBinder;
import io.micronaut.el.processor.compiler.ELFunctionDiscovery;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;

import java.util.Set;

/**
 * Registers the functions declared with {@link ELFunction} in the module, for its expressions.
 *
 * <p>The visitors run in reverse order of {@link #getOrder()}, every class through one visitor before the next,
 * so this visitor sees every class of the round before {@link ELExpressionVisitor} compiles an expression: the
 * functions of the module are known whatever the order of its classes. The functions of another module are
 * listed with {@code @ELFunctions}.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELFunctionVisitor implements TypeElementVisitor<Object, Object> {

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
        if (ELFunctionBinder.declaresFunctions(element)) {
            ELFunctionDiscovery.current().register(element);
        }
    }
}
