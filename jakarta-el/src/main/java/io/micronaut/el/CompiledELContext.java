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
package io.micronaut.el;

import io.micronaut.context.BeanDefinitionRegistry;
import io.micronaut.core.annotation.Experimental;
import org.jspecify.annotations.Nullable;
import io.micronaut.el.runtime.MapVariableMapper;
import io.micronaut.el.resolver.ELResolverChain;
import io.micronaut.el.resolver.ELResolvers;
import jakarta.el.BeanNameELResolver;
import jakarta.el.BeanNameResolver;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.EvaluationListener;
import jakarta.el.FunctionMapper;
import jakarta.el.VariableMapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A standalone {@link ELContext} backed by the resolvers generated at compilation time.
 *
 * <p>The functions of the compiled expressions are bound at compilation time, therefore the function
 * mapper of this context resolves nothing.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public class CompiledELContext extends ELContext {

    private static final FunctionMapper NO_FUNCTIONS = new NoFunctionMapper();

    private final Map<String, Object> beans = new HashMap<>();
    private final ELResolver resolver;
    private final MapVariableMapper variableMapper = new MapVariableMapper();
    private int lambdaScopes;
    private boolean listeners;

    /**
     * Creates a context using the standard chain of resolvers.
     */
    public CompiledELContext() {
        this(new ELResolver[0]);
    }

    /**
     * @param first The resolvers consulted before the standard ones
     */
    public CompiledELContext(ELResolver... first) {
        // a chain nested in a chain is flattened for the coercions
        this.resolver = new ELResolverChain(new BeanNameELResolver(new LocalBeanNameResolver()),
            first.length == 0 ? ELResolvers.standard() : ELResolvers.standard(first));
    }

    /**
     * Creates a context using the standard chain of resolvers of an application that has a bean context, which
     * also invokes the executable methods the beans of that context carry, without the types they are declared
     * on having to be annotated {@code @Introspected}.
     *
     * <p>The registry is also registered on the context itself, under {@code BeanDefinitionRegistry.class},
     * which is where {@code io.micronaut.el.resolver.ExecutableMethodELExecutor} reads it from when it was
     * loaded as a service: an expression parsed at runtime and evaluated with this context reaches the same
     * executable methods as a compiled one.</p>
     *
     * @param registry The registry whose definitions carry the executable methods, which a
     *                 {@code io.micronaut.context.BeanContext} is
     * @param first    The resolvers consulted before the standard ones
     */
    public CompiledELContext(BeanDefinitionRegistry registry, ELResolver... first) {
        this.resolver = new ELResolverChain(new BeanNameELResolver(new LocalBeanNameResolver()),
            ELResolvers.standard(registry, first));
        putContext(BeanDefinitionRegistry.class, registry);
    }

    /**
     * Resolves a declared variable the way {@code ELResolution.resolveVariable} does, from the beans of this
     * context directly when nothing can shadow them: no lambda scope is open and the variable mapper holds no
     * variable.
     *
     * @param name The name of the variable
     * @return The bean, or {@code null} when the name is not a bean of this context or may be shadowed, in which
     * case the resolution goes through the mapper and the resolvers
     */
    @Nullable
    public Object resolveBean(String name) {
        if (lambdaScopes == 0 && variableMapper.isEmpty()) {
            return beans.get(name);
        }
        return null;
    }

    @Override
    public boolean isLambdaArgument(String arg) {
        // the scopes are a stack of maps in the superclass, walked on every identifier otherwise
        return lambdaScopes > 0 && super.isLambdaArgument(arg);
    }

    @Override
    public void addEvaluationListener(EvaluationListener listener) {
        listeners = true;
        super.addEvaluationListener(listener);
    }

    @Override
    public void notifyBeforeEvaluation(String expression) {
        if (listeners) {
            super.notifyBeforeEvaluation(expression);
        }
    }

    @Override
    public void notifyAfterEvaluation(String expression) {
        if (listeners) {
            super.notifyAfterEvaluation(expression);
        }
    }

    @Override
    public void notifyPropertyResolved(@Nullable Object base, @Nullable Object property) {
        if (listeners) {
            super.notifyPropertyResolved(base, property);
        }
    }

    @Override
    public void enterLambdaScope(Map<String, Object> args) {
        lambdaScopes++;
        super.enterLambdaScope(args);
    }

    @Override
    public void exitLambdaScope() {
        lambdaScopes--;
        super.exitLambdaScope();
    }

    /**
     * Defines a bean resolvable by its name.
     *
     * @param name  The name
     * @param value The value
     * @return This context
     */
    public CompiledELContext setBean(String name, @Nullable Object value) {
        beans.put(name, value);
        return this;
    }

    /**
     * @param name The name
     * @return The bean of the given name
     */
    @Nullable
    public Object getBean(String name) {
        return beans.get(name);
    }

    @Override
    public ELResolver getELResolver() {
        return resolver;
    }

    @Override
    public FunctionMapper getFunctionMapper() {
        return NO_FUNCTIONS;
    }

    @Override
    public VariableMapper getVariableMapper() {
        return variableMapper;
    }

    /**
     * The bean name resolver backed by the local beans of the context.
     */
    private final class LocalBeanNameResolver extends BeanNameResolver {

        @Override
        public boolean isNameResolved(String beanName) {
            return beans.containsKey(beanName);
        }

        @Override
        @Nullable
        public Object getBean(String beanName) {
            return beans.get(beanName);
        }

        @Override
        public void setBeanValue(String beanName, Object value) {
            beans.put(beanName, value);
        }

        @Override
        public boolean isReadOnly(String beanName) {
            return false;
        }

        @Override
        public boolean canCreateBean(String beanName) {
            return true;
        }
    }

    /**
     * The variable mapper backed by the local variables of the context.
     */
    private static final class NoFunctionMapper extends FunctionMapper {

        @Override
        @Nullable
        public Method resolveFunction(String prefix, String localName) {
            return null;
        }
    }
}
