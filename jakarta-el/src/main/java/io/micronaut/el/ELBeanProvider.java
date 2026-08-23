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

import io.micronaut.core.annotation.Experimental;

/**
 * Provides the instances the functions declared on beans are invoked on.
 *
 * <p>A function declared with {@code @ELFunctions} on a class with public instance methods is invoked on an
 * instance of the class, which the expression obtains from its {@code jakarta.el.ELContext}: the instance
 * registered with {@code putContext(type, instance)}, or, failing that, the one a provider registered with
 * {@code putContext(ELBeanProvider.class, provider)} returns. A Micronaut application registers its bean context,
 * {@code context.putContext(ELBeanProvider.class, beanContext::getBean)}, so that any bean can declare
 * functions.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
@FunctionalInterface
public interface ELBeanProvider {

    /**
     * @param type The type of the bean
     * @param <T>  The type of the bean
     * @return The instance
     */
    <T> T get(Class<T> type);
}
