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
package io.micronaut.el.resolver;

import jakarta.el.ArrayELResolver;
import jakarta.el.BeanELResolver;
import jakarta.el.CompositeELResolver;
import jakarta.el.ELResolver;
import jakarta.el.ListELResolver;
import jakarta.el.MapELResolver;
import jakarta.el.OptionalELResolver;
import jakarta.el.RecordELResolver;
import jakarta.el.ResourceBundleELResolver;
import jakarta.el.StaticFieldELResolver;

/**
 * The factory of the standard resolver chain of the module.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public final class ELResolvers {

    private ELResolvers() {
    }

    /**
     * Creates the standard chain of resolvers, which starts with the bean introspections generated at
     * compilation time and continues with the resolvers of the specification.
     *
     * @return The resolver chain
     */
    public static ELResolver standard() {
        return standard(new IntrospectionELResolver());
    }

    /**
     * Creates the standard chain of resolvers, starting with the given resolvers.
     *
     * @param first The resolvers to consult first
     * @return The resolver chain
     */
    public static ELResolver standard(ELResolver... first) {
        CompositeELResolver composite = new CompositeELResolver();
        for (ELResolver resolver : first) {
            composite.add(resolver);
        }
        composite.add(new StreamELResolver());
        composite.add(new StaticFieldELResolver());
        composite.add(new MapELResolver());
        composite.add(new ResourceBundleELResolver());
        composite.add(new ListELResolver());
        composite.add(new ArrayELResolver());
        composite.add(new RecordELResolver());
        composite.add(new OptionalELResolver());
        composite.add(new BeanELResolver());
        return composite;
    }
}
