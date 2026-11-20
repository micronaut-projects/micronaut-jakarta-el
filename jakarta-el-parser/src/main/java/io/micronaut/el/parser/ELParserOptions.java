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
package io.micronaut.el.parser;

import io.micronaut.core.annotation.Experimental;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;
import java.util.function.Function;

/**
 * The limits the {@link ELParser} applies to an expression, so that a string built at runtime cannot exhaust the
 * CPU, the memory or the stack of the thread parsing and evaluating it.
 *
 * <p>The options are immutable; each {@code with} method returns a copy with one limit changed:</p>
 *
 * <pre>{@code
 * ELParserOptions options = ELParserOptions.defaults().withMaxLength(4_000_000).withMaxTokens(500_000);
 * }</pre>
 *
 * <p>The limits are:</p>
 * <ul>
 *     <li>{@link #maxLength()}, the number of characters of the expression string;</li>
 *     <li>{@link #maxTokens()}, the number of tokens the string is split into;</li>
 *     <li>{@link #maxDepth()}, how deeply constructs nest inside each other: a parenthesised sub-expression, an
 *     argument, a list, set or map element, an index, the operand of a unary operator, the branches of a choice, the
 *     right side of an assignment and the body of a lambda each nest one level. A chain of left associative
 *     operators, such as {@code a + b + c} or {@code a.b().c().d()}, does not nest, however long it is;</li>
 *     <li>{@link #maxTreeHeight()}, the height of the syntax tree produced. Every consumer of the tree walks it
 *     recursively, so a chain of a thousand operators deepens the tree by a thousand nodes even though it nests
 *     nothing. This bound is far higher than the nesting one, as a node of the tree costs a fraction of the stack
 *     a nesting level of the parser costs.</li>
 * </ul>
 *
 * <p>An expression exceeding a limit is rejected with an {@link ELParsingException}.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Experimental
public final class ELParserOptions implements Serializable {

    /**
     * The number of characters an expression may contain by default.
     */
    public static final int DEFAULT_MAX_LENGTH = 1_048_576;
    /**
     * The number of tokens an expression may contain by default.
     */
    public static final int DEFAULT_MAX_TOKENS = 131_072;
    /**
     * The nesting depth an expression may reach by default, see {@link ELParser#DEFAULT_MAX_DEPTH}.
     */
    public static final int DEFAULT_MAX_DEPTH = ELParser.DEFAULT_MAX_DEPTH;
    /**
     * The height the syntax tree of an expression may reach by default.
     */
    public static final int DEFAULT_MAX_TREE_HEIGHT = 1_000;

    /**
     * The prefix of the system properties {@link #fromProperties(Properties)} reads.
     */
    public static final String PROPERTY_PREFIX = "micronaut.el.parser.";
    /**
     * The property holding {@link #maxLength()}.
     */
    public static final String MAX_LENGTH_PROPERTY = PROPERTY_PREFIX + "max-length";
    /**
     * The property holding {@link #maxTokens()}.
     */
    public static final String MAX_TOKENS_PROPERTY = PROPERTY_PREFIX + "max-tokens";
    /**
     * The property holding {@link #maxDepth()}.
     */
    public static final String MAX_DEPTH_PROPERTY = PROPERTY_PREFIX + "max-depth";
    /**
     * The property holding {@link #maxTreeHeight()}.
     */
    public static final String MAX_TREE_HEIGHT_PROPERTY = PROPERTY_PREFIX + "max-tree-height";

    @Serial
    private static final long serialVersionUID = 1L;

    private static final ELParserOptions DEFAULTS = new ELParserOptions(DEFAULT_MAX_LENGTH, DEFAULT_MAX_TOKENS,
        DEFAULT_MAX_DEPTH, DEFAULT_MAX_TREE_HEIGHT);

    private final int maxLength;
    private final int maxTokens;
    private final int maxDepth;
    private final int maxTreeHeight;

    private ELParserOptions(int maxLength, int maxTokens, int maxDepth, int maxTreeHeight) {
        this.maxLength = positive("maxLength", maxLength);
        this.maxTokens = positive("maxTokens", maxTokens);
        this.maxDepth = positive("maxDepth", maxDepth);
        this.maxTreeHeight = positive("maxTreeHeight", maxTreeHeight);
    }

    /**
     * @return The default limits
     */
    public static ELParserOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Reads the limits from the system properties {@value #MAX_LENGTH_PROPERTY}, {@value #MAX_TOKENS_PROPERTY},
     * {@value #MAX_DEPTH_PROPERTY} and {@value #MAX_TREE_HEIGHT_PROPERTY}, keeping the default of each one that is
     * not set.
     *
     * @return The limits
     */
    public static ELParserOptions fromSystemProperties() {
        return fromProperties(System.getProperties());
    }

    /**
     * Reads the limits from the properties {@value #MAX_LENGTH_PROPERTY}, {@value #MAX_TOKENS_PROPERTY},
     * {@value #MAX_DEPTH_PROPERTY} and {@value #MAX_TREE_HEIGHT_PROPERTY}, keeping the default of each one that is
     * not set.
     *
     * @param properties The properties
     * @return The limits
     */
    public static ELParserOptions fromProperties(Properties properties) {
        return fromResolver(properties::getProperty);
    }

    /**
     * Reads the limits from the given property lookup, for example the environment of an application, keeping
     * the default of each limit the lookup returns {@code null} for.
     *
     * @param lookup Returns the value of a property name such as {@value #MAX_LENGTH_PROPERTY}, or {@code null}
     * @return The limits
     */
    public static ELParserOptions fromResolver(Function<String, @Nullable String> lookup) {
        return new ELParserOptions(
            read(lookup, MAX_LENGTH_PROPERTY, DEFAULT_MAX_LENGTH),
            read(lookup, MAX_TOKENS_PROPERTY, DEFAULT_MAX_TOKENS),
            read(lookup, MAX_DEPTH_PROPERTY, DEFAULT_MAX_DEPTH),
            read(lookup, MAX_TREE_HEIGHT_PROPERTY, DEFAULT_MAX_TREE_HEIGHT));
    }

    /**
     * @return The number of characters an expression may contain
     */
    public int maxLength() {
        return maxLength;
    }

    /**
     * @return The number of tokens an expression may contain
     */
    public int maxTokens() {
        return maxTokens;
    }

    /**
     * @return The depth constructs may nest inside each other
     */
    public int maxDepth() {
        return maxDepth;
    }

    /**
     * @return The height the syntax tree of an expression may reach
     */
    public int maxTreeHeight() {
        return maxTreeHeight;
    }

    /**
     * @param maxLength The number of characters an expression may contain
     * @return A copy of these options with the limit changed
     */
    public ELParserOptions withMaxLength(int maxLength) {
        return new ELParserOptions(maxLength, maxTokens, maxDepth, maxTreeHeight);
    }

    /**
     * @param maxTokens The number of tokens an expression may contain
     * @return A copy of these options with the limit changed
     */
    public ELParserOptions withMaxTokens(int maxTokens) {
        return new ELParserOptions(maxLength, maxTokens, maxDepth, maxTreeHeight);
    }

    /**
     * Changes the nesting depth. A level of nesting costs around fifteen frames of the recursive descent, so a
     * depth much beyond the default needs a thread with a larger stack.
     *
     * @param maxDepth The depth constructs may nest inside each other
     * @return A copy of these options with the limit changed
     */
    public ELParserOptions withMaxDepth(int maxDepth) {
        return new ELParserOptions(maxLength, maxTokens, maxDepth, maxTreeHeight);
    }

    /**
     * Changes the height of the syntax tree. The default holds on a thread whose stack is half of the usual one;
     * every node of the tree costs a few frames of each recursive walk of it, so a much taller tree needs a thread
     * with a larger stack.
     *
     * @param maxTreeHeight The height the syntax tree of an expression may reach
     * @return A copy of these options with the limit changed
     */
    public ELParserOptions withMaxTreeHeight(int maxTreeHeight) {
        return new ELParserOptions(maxLength, maxTokens, maxDepth, maxTreeHeight);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof ELParserOptions other
            && maxLength == other.maxLength
            && maxTokens == other.maxTokens
            && maxDepth == other.maxDepth
            && maxTreeHeight == other.maxTreeHeight;
    }

    @Override
    public int hashCode() {
        int result = maxLength;
        result = 31 * result + maxTokens;
        result = 31 * result + maxDepth;
        return 31 * result + maxTreeHeight;
    }

    @Override
    public String toString() {
        return "ELParserOptions[maxLength=" + maxLength + ", maxTokens=" + maxTokens + ", maxDepth=" + maxDepth
            + ", maxTreeHeight=" + maxTreeHeight + "]";
    }

    private static int read(Function<String, @Nullable String> lookup, String name, int defaultValue) {
        String value = lookup.apply(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim().replace("_", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The property " + name + " must be an integer, but was: " + value, e);
        }
    }

    private static int positive(String name, int value) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive, but was: " + value);
        }
        return value;
    }
}
