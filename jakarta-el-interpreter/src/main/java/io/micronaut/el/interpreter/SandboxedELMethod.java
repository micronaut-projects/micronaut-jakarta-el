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
package io.micronaut.el.interpreter;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELSandbox;
import io.micronaut.el.runtime.ELSandboxedResolution;
import jakarta.el.ELContext;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.Annotation;

/**
 * A method a {@link io.micronaut.el.ELMethodExecutor#isReflective() reflective executor} resolved for an
 * expression parsed at runtime, invoked under the {@link ELSandbox} of the context of each invocation: the base
 * object is checked before the method runs, and the value it returned after.
 *
 * <p>A call site keeps the method it resolved and invokes it again on later evaluations, possibly with another
 * context, so the sandbox travels with the method rather than being consulted once when it was found.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
final class SandboxedELMethod implements ELMethod {

    private static final long serialVersionUID = 1L;

    private final ELMethod method;

    /**
     * @param method The method the reflective executor resolved
     */
    SandboxedELMethod(ELMethod method) {
        this.method = method;
    }

    /**
     * Fails when the sandbox of the context denies the base object the method would be reached on.
     *
     * @param context The context
     * @param base    The base object, an {@link jakarta.el.ELClass} for a static method or a constructor
     */
    void checkAccess(ELContext context, Object base) {
        ELSandboxedResolution.checkAccess(ELSandbox.of(context), base);
    }

    @Override
    @Nullable
    public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
        ELSandbox sandbox = ELSandbox.of(context);
        if (base != null) {
            ELSandboxedResolution.checkAccess(sandbox, base);
        }
        return ELSandboxedResolution.checkValue(sandbox, method.invoke(context, base, arguments));
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    // the wildcard is the description of the method's return type, as in the interface
    @SuppressWarnings("java:S1452")
    public Argument<?> getReturnType() {
        return method.getReturnType();
    }

    @Override
    // the parameters of a method are of different types, which the wildcard is what describes
    @SuppressWarnings("java:S1452")
    public Argument<?>[] getArguments() {
        return method.getArguments();
    }

    @Override
    public boolean isVarArgs() {
        return method.isVarArgs();
    }

    @Override
    public AnnotationMetadata getAnnotationMetadata() {
        return method.getAnnotationMetadata();
    }

    @Override
    public Annotation[] synthesizeAnnotations() {
        return method.synthesizeAnnotations();
    }

    @Override
    public boolean isReusable() {
        return method.isReusable();
    }

    @Override
    public String identity() {
        return method.identity();
    }

    @Override
    public String toString() {
        return method.toString();
    }
}
