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
import org.jspecify.annotations.Nullable;

/**
 * Detects calls whose synthetic Java bridge method changes Jakarta EL overload selection.
 */
final class ELBridgeMethods {

    private ELBridgeMethods() {
    }

    /**
     * Returns whether runtime selection must account for a {@link Comparable#compareTo(Object)} bridge method.
     * Reflection sees the bridge before considering coercion, so directly invoking the generic target would
     * incorrectly accept an argument such as a number for {@code String.compareTo}.
     */
    static boolean requiresRuntimeSelection(ClassElement baseType,
                                            MethodElement target,
                                            @Nullable ClassElement argument) {
        if (!target.getName().equals("compareTo") || target.getParameters().length != 1
            || !baseType.isAssignable(Comparable.class) || argument == null) {
            return false;
        }
        ClassElement parameter = target.getParameters()[0].getType();
        return !argument.getName().equals(parameter.getName())
            && !argument.isAssignable(parameter)
            && !parameter.getName().equals(Object.class.getName());
    }
}
