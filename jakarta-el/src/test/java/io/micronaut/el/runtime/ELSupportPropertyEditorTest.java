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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.beans.PropertyEditorManager;
import java.beans.PropertyEditorSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The coercion of a String to a type without a built-in coercion, which the section 1.23.7 of the specification
 * performs through the {@code PropertyEditor} of the type. This is a contract of the runtime API with no
 * expression text of its own, so it has no generated counterpart.
 */
class ELSupportPropertyEditorTest {

    @BeforeAll
    static void registerEditor() {
        PropertyEditorManager.registerEditor(Point.class, PointEditor.class);
    }

    @Test
    void aStringIsCoercedByTheEditorOfTheType() {
        assertEquals(new Point(1, 2), ELSupport.coerce("1,2", Point.class));
    }

    @Test
    void anEmptyStringTheEditorRejectsIsNull() {
        assertNull(ELSupport.coerce("", Point.class));
    }

    @Test
    void aStringTheEditorRejectsCannotBeCoerced() {
        assertThrows(ELException.class, () -> ELSupport.coerce("not a point", Point.class));
    }

    /**
     * A type with an editor, and no coercion of the specification.
     *
     * @param x The abscissa
     * @param y The ordinate
     */
    public record Point(int x, int y) {
    }

    /**
     * Reads a point written as {@code x,y}.
     */
    public static final class PointEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) {
            String[] parts = text.split(",");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Not a point: " + text);
            }
            setValue(new Point(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())));
        }
    }
}
