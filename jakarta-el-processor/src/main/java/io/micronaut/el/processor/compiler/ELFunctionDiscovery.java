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

import io.micronaut.core.annotation.Internal;
import io.micronaut.el.annotation.ELFunctionIndex;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.VisitorContext;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The functions declared with {@code @ELFunction} that no expression had to list: the ones of the classes of
 * the current compilation, registered as they are visited, and the ones of the classpath, found through the
 * index classes generated in {@link ELFunctionIndex#PACKAGE}.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELFunctionDiscovery {

    private static final ELFunctionDiscovery CURRENT = new ELFunctionDiscovery();

    private final Map<String, MethodElement> functions = new LinkedHashMap<>();
    private final Set<String> registered = new HashSet<>();
    private boolean classpathScanned;

    /**
     * @return The discovery of the current compilation, shared by the visitors
     */
    public static ELFunctionDiscovery current() {
        return CURRENT;
    }

    /**
     * Registers the functions a class of the current compilation declares.
     *
     * @param type The class
     * @return False if the class was registered already
     */
    public boolean register(ClassElement type) {
        if (!registered.add(type.getName())) {
            return false;
        }
        ELFunctionBinder.bind(type, "", true, functions);
        return true;
    }

    /**
     * @param context       The visitor context
     * @param qualifiedName The qualified name of the function
     * @return The function, or null when none is declared under the name
     */
    @Nullable
    public MethodElement find(VisitorContext context, String qualifiedName) {
        MethodElement function = functions.get(qualifiedName);
        if (function == null && !classpathScanned) {
            classpathScanned = true;
            for (ClassElement index : context.getClassElements(ELFunctionIndex.PACKAGE, ELFunctionIndex.class.getName())) {
                index.stringValue(ELFunctionIndex.class)
                    .flatMap(context::getClassElement)
                    .ifPresent(declaring -> ELFunctionBinder.bind(declaring, "", true, functions));
            }
            function = functions.get(qualifiedName);
        }
        return function;
    }

    /**
     * Forgets everything, at the start of a compilation.
     */
    public void reset() {
        functions.clear();
        registered.clear();
        classpathScanned = false;
    }
}
