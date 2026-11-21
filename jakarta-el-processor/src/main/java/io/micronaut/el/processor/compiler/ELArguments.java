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

import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.sourcegen.model.ExpressionDef;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;

/**
 * The arguments of an invocation, each compiled once, the first time the selection of the method or the
 * invocation reads it: a compiled argument is an expression placed in the invocation, and compiling it again
 * would only add unused helpers.
 */
final class ELArguments {

    private final List<ELNode> nodes;
    private final ExpressionDef ctx;
    private final BiFunction<ELNode, ExpressionDef, ELCompiler.Typed> compiler;
    private final ELCompiler.Typed[] compiled;

    /**
     * @param nodes    The arguments
     * @param ctx      The context the arguments are compiled in
     * @param compiler The compilation of an argument
     */
    ELArguments(List<ELNode> nodes, ExpressionDef ctx, BiFunction<ELNode, ExpressionDef, ELCompiler.Typed> compiler) {
        this.nodes = nodes;
        this.ctx = ctx;
        this.compiler = compiler;
        this.compiled = new ELCompiler.Typed[nodes.size()];
    }

    int size() {
        return nodes.size();
    }

    ELNode node(int index) {
        return nodes.get(index);
    }

    List<ELNode> nodes() {
        return nodes;
    }

    ELCompiler.Typed typed(int index) {
        ELCompiler.Typed typed = compiled[index];
        if (typed == null) {
            typed = compiler.apply(nodes.get(index), ctx);
            compiled[index] = typed;
        }
        return typed;
    }

    /** @return The static type of the argument, null for a lambda expression, whose type is its target */
    @Nullable
    ClassElement type(int index) {
        return nodes.get(index) instanceof ELNode.Lambda ? null : typed(index).type();
    }

    List<@Nullable ClassElement> types() {
        List<@Nullable ClassElement> types = new java.util.ArrayList<>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            types.add(type(i));
        }
        return types;
    }

    ExpressionDef ctx() {
        return ctx;
    }
}
