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

import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Orders two candidate methods by how specific their parameters are, which is how the compiler picks the
 * overload the section 1.6 of the specification asks for, and the section 15.12.2.5 of the Java Language
 * Specification defines.
 */
final class ELSpecificity {

    private ELSpecificity() {
    }

    // the specificity comparison of the section 15.12.2.5 of the JLS: one ordered sequence of tests
    @SuppressWarnings("java:S3776")
    static int compare(MethodElement first,
                                          MethodElement second,
                                          List<ClassElement> argumentTypes,
                                          boolean elSpecific) {
        int length = Math.max(Math.max(first.getParameters().length, second.getParameters().length), argumentTypes.size());
        int result = 0;
        for (int i = 0; i < length; i++) {
            ClassElement firstType = comparisonType(first, i);
            ClassElement secondType = comparisonType(second, i);
            if (sameBoxedType(firstType, secondType)) {
                continue;
            }
            int comparison = firstType.isAssignable(secondType) ? 1
                : secondType.isAssignable(firstType) ? -1
                : numericSpecificity(firstType, secondType,
                    i < argumentTypes.size() ? argumentTypes.get(i) : null, elSpecific);
            if (comparison == 0 || (result != 0 && result != comparison)) {
                return 0;
            }
            result = comparison;
        }
        return result;
    }

    static ClassElement comparisonType(MethodElement method, int index) {
        ParameterElement[] parameters = method.getParameters();
        if (method.isVarArgs() && index >= parameters.length - 1) {
            return parameters[parameters.length - 1].getType().fromArray();
        }
        return parameters[index].getType();
    }

    private static int numericSpecificity(ClassElement first,
                                          ClassElement second,
                                          @Nullable ClassElement argument,
                                          boolean elSpecific) {
        if (!elSpecific || argument == null || ELCompiler.numericRank(argument) < 0) {
            return 0;
        }
        boolean firstNumeric = ELCompiler.numericRank(first) >= 0 || first.isAssignable(Number.class);
        boolean secondNumeric = ELCompiler.numericRank(second) >= 0 || second.isAssignable(Number.class);
        return firstNumeric == secondNumeric ? 0 : firstNumeric ? 1 : -1;
    }

    static boolean sameBoxedType(ClassElement first, ClassElement second) {
        return first.getName().equals(second.getName())
            || first.getName().equals(ELCompiler.wrapper(second.getName()))
            || ELCompiler.wrapper(first.getName()).equals(second.getName());
    }
}
