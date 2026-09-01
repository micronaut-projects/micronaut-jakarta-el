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
package io.micronaut.el.interpreter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterpretingELExpressionParserTest {

    @Test
    void parsedExpressionCacheEvictsOnlyTheLeastRecentlyUsedEntry() {
        InterpretingELExpressionParser parser = new InterpretingELExpressionParser();
        for (int i = 0; i < InterpretingELExpressionParser.CACHE_SIZE; i++) {
            parser.createValueExpression(null, "${" + i + "}", Object.class);
        }
        parser.createValueExpression(null, "${0}", Object.class);

        parser.createValueExpression(null, "${extra}", Object.class);

        assertEquals(InterpretingELExpressionParser.CACHE_SIZE, parser.cachedExpressions());
        assertTrue(parser.isCached("${0}"));
        assertFalse(parser.isCached("${1}"));
    }
}
