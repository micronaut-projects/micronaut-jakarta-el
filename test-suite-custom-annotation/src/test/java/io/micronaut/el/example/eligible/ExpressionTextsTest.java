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
package io.micronaut.el.example.eligible;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The text the remappers store for an expression. It runs inside the compiler of the consuming modules, where
 * the coverage agent does not see it; the expressions it stores are evaluated by the tests of
 * {@code test-suite-java}. This checks the text transformation alone, which evaluates no expression.
 */
class ExpressionTextsTest {

    @Test
    void everyImmediateDelimiterIsStoredAsADeferredOne() {
        assertEquals("#{a}", ExpressionTexts.deferred("${a}"));
        assertEquals("x #{a} y #{b}", ExpressionTexts.deferred("x ${a} y ${b}"));
        assertEquals("#{a}", ExpressionTexts.deferred("#{a}"));
    }

    @Test
    void anEscapedDelimiterIsKeptAsItIs() {
        assertEquals("\\${a} #{b}", ExpressionTexts.deferred("\\${a} ${b}"));
        assertEquals("\\#{a}", ExpressionTexts.deferred("\\#{a}"));
        // the escaped character is consumed with the backslash, so the brace after it is plain text
        assertEquals("\\${", ExpressionTexts.deferred("\\${"));
    }

    @Test
    void aDollarOrABackslashThatStartsNoDelimiterIsPlainText() {
        assertEquals("", ExpressionTexts.deferred(""));
        assertEquals("$a \\n", ExpressionTexts.deferred("$a \\n"));
        assertEquals("cost: 5$", ExpressionTexts.deferred("cost: 5$"));
        assertEquals("trailing \\", ExpressionTexts.deferred("trailing \\"));
        // a backslash escapes the delimiter even after another backslash
        assertEquals("\\\\${a}", ExpressionTexts.deferred("\\\\${a}"));
    }
}
