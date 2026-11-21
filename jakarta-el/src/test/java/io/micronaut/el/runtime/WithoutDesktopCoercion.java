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

import jakarta.el.ELException;

/**
 * The program {@link ELSupportWithoutDesktopTest} runs in a JVM whose boot layer leaves out {@code java.desktop}:
 * it reports, one line each, whether the module is there, what the coercion of a String to a type without a
 * built-in coercion throws, and what the coercion of a String to an {@code Integer} returns. Any other failure
 * escapes and exits the JVM with a non-zero status.
 */
public final class WithoutDesktopCoercion {

    private WithoutDesktopCoercion() {
    }

    public static void main(String[] args) {
        System.out.println("desktop=" + ModuleLayer.boot().findModule("java.desktop").isPresent());
        try {
            Object coerced = ELSupport.coerce("x", WithoutDesktopCoercion.class);
            System.out.println("coerced=" + coerced);
        } catch (ELException e) {
            System.out.println("failure=" + e.getClass().getName() + ": " + e.getMessage());
        }
        // a second time, once the missing module is known
        try {
            ELSupport.coerce("y", WithoutDesktopCoercion.class);
        } catch (ELException e) {
            System.out.println("again=" + e.getClass().getName());
        }
        Integer number = ELSupport.coerce("42", Integer.class);
        System.out.println("integer=" + number);
    }
}
