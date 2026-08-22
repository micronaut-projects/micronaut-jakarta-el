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

import org.jspecify.annotations.Nullable;
import io.micronaut.el.resolver.IntrospectionELResolver;
import io.micronaut.el.resolver.ELResolvers;
import jakarta.el.BeanNameELResolver;
import jakarta.el.BeanNameResolver;
import jakarta.el.CompositeELResolver;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.FunctionMapper;
import jakarta.el.ValueExpression;
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
public class CompiledELContext extends ELContext {

    private final Map<String, Object> beans = new HashMap<>();
    private final Map<String, ValueExpression> variables = new HashMap<>();
    private final ELResolver resolver;
    private final FunctionMapper functionMapper = new NoFunctionMapper();
    private final VariableMapper variableMapper = new MapVariableMapper();

    /**
     * Creates a context using the standard chain of resolvers.
     */
    public CompiledELContext() {
        this(new IntrospectionELResolver());
    }

    /**
     * @param first The resolvers consulted before the standard ones
     */
    public CompiledELContext(ELResolver... first) {
        CompositeELResolver composite = new CompositeELResolver();
        composite.add(new BeanNameELResolver(new LocalBeanNameResolver()));
        composite.add(ELResolvers.standard(first));
        this.resolver = composite;
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
        return functionMapper;
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
    private final class MapVariableMapper extends VariableMapper {

        @Override
        @Nullable
        public ValueExpression resolveVariable(String variable) {
            return variables.get(variable);
        }

        @Override
        @Nullable
        public ValueExpression setVariable(String variable, @Nullable ValueExpression expression) {
            return expression == null ? variables.remove(variable) : variables.put(variable, expression);
        }
    }

    /**
     * The function mapper of a context whose expressions bind their functions at compilation time.
     */
    private static final class NoFunctionMapper extends FunctionMapper {

        @Override
        @Nullable
        public Method resolveFunction(String prefix, String localName) {
            return null;
        }
    }
}
