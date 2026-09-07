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

import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.TypeDef;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes invocations of the runtime methods with their erased JVM signatures.
 */
final class ELRuntimeInvocation {

    private ELRuntimeInvocation() {
    }

    static ExpressionDef invoke(ClassTypeDef owner,
                                String name,
                                TypeDef returning,
                                List<? extends ExpressionDef> values) {
        Method method = runtimeMethod(owner, name, values.size());
        if (method == null) {
            return owner.invokeStatic(name, returning, values);
        }
        Class<?>[] parameters = method.getParameterTypes();
        List<TypeDef> parameterTypes = new ArrayList<>(parameters.length);
        for (Class<?> parameter : parameters) {
            parameterTypes.add(TypeDef.of(parameter));
        }
        List<ExpressionDef> arguments = new ArrayList<>(parameters.length);
        boolean variadic = method.isVarArgs()
            && (values.size() != parameters.length
            || !(values.get(values.size() - 1).type() instanceof TypeDef.Array));
        int fixed = variadic ? parameters.length - 1 : parameters.length;
        arguments.addAll(values.subList(0, fixed));
        if (variadic) {
            Class<?> componentType = parameters[parameters.length - 1].getComponentType();
            arguments.add(TypeDef.of(componentType).array().instantiate(values.subList(fixed, values.size())));
        }
        TypeDef declaredReturn = TypeDef.of(method.getReturnType());
        ExpressionDef invocation = owner.invokeStatic(name, parameterTypes, declaredReturn, arguments);
        return declaredReturn.equals(returning) ? invocation : invocation.cast(returning);
    }

    /**
     * The static method the runtime declares, read from the class on the compile classpath.
     *
     * <p>The model of the compiler would describe the same method, and {@code ClassElement} was tried here:
     * it works under javac and not under the Kotlin or the Groovy processor, which report the variable arity
     * parameter of a binary Java method differently, so the emitted call lost its packing. The class itself
     * answers the same way for all three. This runs while compiling and never at runtime, which is why the
     * reflection boundary of the runtime does not cover this module.</p>
     */
    @Nullable
    private static Method runtimeMethod(ClassTypeDef owner, String name, int argumentCount) {
        Class<?> runtime = ClassUtils.forName(owner.getName(), ELRuntimeInvocation.class.getClassLoader()).orElse(null);
        if (runtime == null) {
            return null;
        }
        for (Method method : runtime.getMethods()) {
            if (!method.getName().equals(name) || !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            int parameters = method.getParameterCount();
            if (argumentCount == parameters || (method.isVarArgs() && argumentCount >= parameters - 1)) {
                return method;
            }
        }
        return null;
    }
}
