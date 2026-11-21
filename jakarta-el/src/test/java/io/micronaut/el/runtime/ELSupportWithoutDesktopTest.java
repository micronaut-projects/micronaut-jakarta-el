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

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The coercion of a String to a type without a built-in coercion goes through the {@code java.beans} property
 * editors, which live in the {@code java.desktop} module that a runtime image built with {@code jlink} may leave
 * out. This runs a JVM whose boot layer really lacks the module, with the runtime classpath of the tests, and
 * checks that such a coercion fails as the coercion of a type without an editor does, with an
 * {@link jakarta.el.ELException}, rather than with a {@link NoClassDefFoundError}, and that the coercions that do
 * not need an editor still work there.
 *
 * <p>This is a contract of the runtime API with no expression, so it has no generated counterpart.</p>
 */
class ELSupportWithoutDesktopTest {

    /**
     * The modules the classpath needs, which are all but {@code java.desktop} and the modules requiring it.
     */
    private static final String MODULES = "java.base,java.logging,java.naming,java.sql,java.management,"
        + "java.xml,jdk.unsupported";

    @Test
    void aStringIsNotCoercedToATypeWithoutAnEditorWhenTheEditorsAreMissing() throws Exception {
        String java = ProcessHandle.current().info().command().orElseThrow();
        Path output = Files.createTempFile("el-without-desktop", ".txt");
        try {
            Process process = new ProcessBuilder(List.of(java,
                "--limit-modules", MODULES,
                "-cp", System.getProperty("java.class.path"),
                WithoutDesktopCoercion.class.getName()))
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start();
            assertTrue(process.waitFor(2, TimeUnit.MINUTES), "the child JVM did not exit");
            String text = Files.readString(output, StandardCharsets.UTF_8);

            assertEquals(0, process.exitValue(), text);
            assertTrue(text.contains("desktop=false"), text);
            assertTrue(text.contains("failure=jakarta.el.ELException: "), text);
            assertTrue(text.contains(WithoutDesktopCoercion.class.getName()), text);
            assertTrue(text.contains("again=jakarta.el.ELException"), text);
            assertTrue(text.contains("integer=42"), text);
            assertFalse(text.contains("NoClassDefFoundError"), text);
            assertFalse(text.contains("coerced="), text);
        } finally {
            Files.deleteIfExists(output);
        }
    }
}
