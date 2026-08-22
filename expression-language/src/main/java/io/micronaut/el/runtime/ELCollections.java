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
package io.micronaut.el.runtime;

import io.micronaut.core.annotation.Internal;
import jakarta.el.ELException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The construction of the collection objects described in the section 2.2 of the Jakarta Expression
 * Language specification.
 *
 * <p>This class is invoked by the expressions generated at compilation time, it is not part of the public
 * API of the module.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELCollections {

    private ELCollections() {
    }

    /**
     * @param elements The elements
     * @return The constructed list
     */
    public static List<Object> list(Object... elements) {
        List<Object> list = new ArrayList<>(elements.length);
        for (Object element : elements) {
            list.add(element);
        }
        return list;
    }

    /**
     * @param elements The elements
     * @return The constructed set
     */
    public static Set<Object> set(Object... elements) {
        Set<Object> set = new LinkedHashSet<>(elements.length);
        for (Object element : elements) {
            set.add(element);
        }
        return set;
    }

    /**
     * @param keysAndValues The keys and the values, in the order they appear in the expression
     * @return The constructed map
     */
    public static Map<Object, Object> map(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new ELException("A map construction requires a value for every key");
        }
        Map<Object, Object> map = new LinkedHashMap<>(keysAndValues.length / 2);
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
