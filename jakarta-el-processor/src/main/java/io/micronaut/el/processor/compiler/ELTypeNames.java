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

/**
 * The type names the compiler compares against, which it reads from the model as strings rather than loading
 * the classes they name.
 */
final class ELTypeNames {

    static final String JAVA_LANG_BYTE = "java.lang.Byte";
    static final String JAVA_LANG_SHORT = "java.lang.Short";
    static final String JAVA_LANG_INTEGER = "java.lang.Integer";
    static final String JAVA_LANG_LONG = "java.lang.Long";
    static final String JAVA_LANG_FLOAT = "java.lang.Float";
    static final String JAVA_LANG_DOUBLE = "java.lang.Double";
    static final String JAKARTA_EL_ELCLASS = "jakarta.el.ELClass";

    private ELTypeNames() {
    }
}
