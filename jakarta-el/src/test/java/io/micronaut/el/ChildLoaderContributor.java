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

/**
 * A contributor whose service descriptor is only reachable through the child class loader of
 * {@link RuntimeRegressionCoreTest}: the class is on the test classpath, the descriptor under
 * {@code child-loader-contributor/META-INF/services} is not.
 */
public final class ChildLoaderContributor implements ELMethodContributor {

    @Override
    public void contribute(ELMethodRegistry registry) {
        registry.functionalInterface(Shout.class, (context, lambda) -> value -> String.valueOf(lambda.invoke(value)));
    }

    /**
     * The functional interface the contributor describes.
     */
    @FunctionalInterface
    public interface Shout {

        String shout(String value);
    }
}
