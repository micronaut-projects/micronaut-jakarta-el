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
package io.micronaut.el.stream;

import org.jspecify.annotations.Nullable;
import io.micronaut.core.annotation.Internal;
import io.micronaut.el.runtime.ELArithmetic;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import jakarta.el.MethodNotFoundException;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * The {@code Stream} implementation class described in the section 2.3.3.1 of the Jakarta Expression
 * Language specification.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELStream {

    private final ELContext context;
    private final Stream<Object> stream;

    private ELStream(ELContext context, Stream<Object> stream) {
        this.context = context;
        this.stream = stream;
    }

    /**
     * Creates a stream from a {@link Collection} or a Java array.
     *
     * @param context The context
     * @param source  The source of the stream
     * @return The stream
     */
    public static ELStream of(ELContext context, Object source) {
        if (source instanceof Collection<?> collection) {
            return new ELStream(context, new ArrayList<Object>(collection).stream());
        }
        if (source.getClass().isArray()) {
            int length = Array.getLength(source);
            List<Object> elements = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                elements.add(Array.get(source, i));
            }
            return new ELStream(context, elements.stream());
        }
        throw new ELException("Cannot create a stream from " + source.getClass().getName());
    }

    /**
     * @param predicate The predicate
     * @return A stream of the elements matching the predicate
     */
    public ELStream filter(LambdaExpression predicate) {
        return next(stream.filter(element -> ELSupport.toBoolean(invoke(predicate, element))));
    }

    /**
     * @param mapper The mapper
     * @return A stream of the mapped elements
     */
    public ELStream map(LambdaExpression mapper) {
        return next(stream.map(element -> invoke(mapper, element)));
    }

    /**
     * @param mapper The mapper producing a stream for every element
     * @return The concatenation of the mapped streams
     */
    public ELStream flatMap(LambdaExpression mapper) {
        return next(stream.flatMap(element -> {
            Object mapped = invoke(mapper, element);
            if (mapped instanceof ELStream elStream) {
                return elStream.stream;
            }
            throw new ELException("The flatMap function must return a stream");
        }));
    }

    /**
     * @return A stream of the distinct elements
     */
    public ELStream distinct() {
        return next(stream.distinct());
    }

    /**
     * @return A stream of the elements sorted in their natural order
     */
    public ELStream sorted() {
        return next(stream.sorted(ELSupport::compare));
    }

    /**
     * @param comparator The comparator
     * @return A stream of the elements sorted with the comparator
     */
    public ELStream sorted(LambdaExpression comparator) {
        return next(stream.sorted((left, right) ->
            intValue(invoke(comparator, left, right))));
    }

    /**
     * @param consumer The consumer invoked for every element
     * @return Always {@code null}
     */
    @Nullable
    public Object forEach(LambdaExpression consumer) {
        stream.forEach(element -> invoke(consumer, element));
        return null;
    }

    /**
     * @param consumer The consumer invoked for every element
     * @return A stream of the same elements
     */
    public ELStream peek(LambdaExpression consumer) {
        return next(stream.peek(element -> invoke(consumer, element)));
    }

    /**
     * @return An iterator over the elements of the stream
     */
    public Iterator<Object> iterator() {
        return stream.iterator();
    }

    /**
     * @param count The maximum number of elements
     * @return A stream of at most {@code count} elements
     */
    public ELStream limit(Object count) {
        return next(stream.limit(Math.max(0, longValue(count))));
    }

    /**
     * @param start The number of elements to skip
     * @return A stream skipping the first elements
     */
    public ELStream substream(Object start) {
        return next(stream.skip(Math.max(0, longValue(start))));
    }

    /**
     * @param start The number of elements to skip
     * @param end   The exclusive end position
     * @return A stream of the elements between the two positions
     */
    public ELStream substream(Object start, Object end) {
        long from = Math.max(0, longValue(start));
        long to = Math.max(from, longValue(end));
        return next(stream.skip(from).limit(to - from));
    }

    /**
     * @return An array of the elements of the stream
     */
    public Object[] toArray() {
        return stream.toArray();
    }

    /**
     * @return A list of the elements of the stream
     */
    public List<Object> toList() {
        return stream.collect(java.util.stream.Collectors.toList());
    }

    /**
     * @param binaryOperator The accumulator
     * @return An optional holding the reduced value
     */
    public ELOptional reduce(LambdaExpression binaryOperator) {
        return ELOptional.of(stream.reduce((left, right) -> invoke(binaryOperator, left, right)).orElse(null));
    }

    /**
     * @param seed           The seed
     * @param binaryOperator The accumulator
     * @return The reduced value
     */
    @Nullable
    public Object reduce(@Nullable Object seed, LambdaExpression binaryOperator) {
        Object accumulator = seed;
        for (Iterator<Object> iterator = stream.iterator(); iterator.hasNext();) {
            accumulator = invoke(binaryOperator, accumulator, iterator.next());
        }
        return accumulator;
    }

    /**
     * @return An optional holding the maximum element
     */
    public ELOptional max() {
        return ELOptional.of(stream.max(ELSupport::compare).orElse(null));
    }

    /**
     * @param comparator The comparator
     * @return An optional holding the maximum element
     */
    public ELOptional max(LambdaExpression comparator) {
        return ELOptional.of(stream.max((left, right) ->
            intValue(invoke(comparator, left, right))).orElse(null));
    }

    /**
     * @return An optional holding the minimum element
     */
    public ELOptional min() {
        return ELOptional.of(stream.min(ELSupport::compare).orElse(null));
    }

    /**
     * @param comparator The comparator
     * @return An optional holding the minimum element
     */
    public ELOptional min(LambdaExpression comparator) {
        return ELOptional.of(stream.min((left, right) ->
            intValue(invoke(comparator, left, right))).orElse(null));
    }

    /**
     * @return An optional holding the average of the elements
     */
    public ELOptional average() {
        Object sum = Long.valueOf(0);
        long count = 0;
        for (Iterator<Object> iterator = stream.iterator(); iterator.hasNext();) {
            sum = ELArithmetic.add(sum, iterator.next());
            count++;
        }
        if (count == 0) {
            return ELOptional.empty();
        }
        return ELOptional.of(ELArithmetic.divide(sum, Long.valueOf(count)));
    }

    /**
     * @return The sum of the elements, zero for an empty stream
     */
    public Object sum() {
        Object sum = Long.valueOf(0);
        for (Iterator<Object> iterator = stream.iterator(); iterator.hasNext();) {
            sum = ELArithmetic.add(sum, iterator.next());
        }
        return sum;
    }

    /**
     * @return The number of elements of the stream
     */
    public Long count() {
        return Long.valueOf(stream.count());
    }

    /**
     * @param predicate The predicate
     * @return An optional holding true when any element matches, empty for an empty stream
     */
    public ELOptional anyMatch(LambdaExpression predicate) {
        return match(predicate, MatchKind.ANY);
    }

    /**
     * @param predicate The predicate
     * @return An optional holding true when all the elements match, empty for an empty stream
     */
    public ELOptional allMatch(LambdaExpression predicate) {
        return match(predicate, MatchKind.ALL);
    }

    /**
     * @param predicate The predicate
     * @return An optional holding true when no element matches, empty for an empty stream
     */
    public ELOptional noneMatch(LambdaExpression predicate) {
        return match(predicate, MatchKind.NONE);
    }

    /**
     * @return An optional holding the first element of the stream
     */
    public ELOptional findFirst() {
        return ELOptional.of(stream.findFirst().orElse(null));
    }

    /**
     * Invokes one of the operations of this class, avoiding the reflective dispatch of the standard
     * resolvers.
     *
     * @param name      The name of the operation
     * @param arguments The arguments
     * @return The result of the operation
     */
    @Nullable
    @SuppressWarnings("java:S1479")
    public Object invokeOperation(String name, Object[] arguments) {
        return switch (name) {
            case "filter" -> filter(lambda(context, arguments, 0, name));
            case "map" -> map(lambda(context, arguments, 0, name));
            case "flatMap" -> flatMap(lambda(context, arguments, 0, name));
            case "distinct" -> distinct();
            case "sorted" -> arguments.length == 0 ? sorted() : sorted(lambda(context, arguments, 0, name));
            case "forEach" -> forEach(lambda(context, arguments, 0, name));
            case "peek" -> peek(lambda(context, arguments, 0, name));
            case "iterator" -> iterator();
            case "limit" -> limit(argument(arguments, 0, name));
            case "substream" -> arguments.length == 1
                ? substream(argument(arguments, 0, name))
                : substream(argument(arguments, 0, name), argument(arguments, 1, name));
            case "toArray" -> toArray();
            case "toList" -> toList();
            case "reduce" -> arguments.length == 1
                ? reduce(lambda(context, arguments, 0, name))
                : reduce(argument(arguments, 0, name), lambda(context, arguments, 1, name));
            case "max" -> arguments.length == 0 ? max() : max(lambda(context, arguments, 0, name));
            case "min" -> arguments.length == 0 ? min() : min(lambda(context, arguments, 0, name));
            case "average" -> average();
            case "sum" -> sum();
            case "count" -> count();
            case "anyMatch" -> anyMatch(lambda(context, arguments, 0, name));
            case "allMatch" -> allMatch(lambda(context, arguments, 0, name));
            case "noneMatch" -> noneMatch(lambda(context, arguments, 0, name));
            case "findFirst" -> findFirst();
            default -> throw new MethodNotFoundException("Unknown stream operation '" + name + "'");
        };
    }

    static LambdaExpression lambda(ELContext context, Object[] arguments, int index, String operation) {
        Object argument = argument(arguments, index, operation);
        if (argument instanceof LambdaExpression lambdaExpression) {
            lambdaExpression.setELContext(context);
            return lambdaExpression;
        }
        throw new ELException("The operation '" + operation + "' expects a lambda expression as the argument "
            + (index + 1));
    }

    static Object argument(Object[] arguments, int index, String operation) {
        if (index >= arguments.length) {
            throw new ELException("The operation '" + operation + "' expects at least " + (index + 1)
                + " argument(s)");
        }
        Object argument = arguments[index];
        if (argument == null) {
            throw new NullPointerException("The argument " + (index + 1) + " of the operation '" + operation
                + "' is null");
        }
        return argument;
    }

    private ELOptional match(LambdaExpression predicate, MatchKind kind) {
        boolean empty = true;
        boolean anyMatched = false;
        boolean allMatched = true;
        for (Iterator<Object> iterator = stream.iterator(); iterator.hasNext();) {
            empty = false;
            boolean matched = ELSupport.toBoolean(invoke(predicate, iterator.next()));
            anyMatched |= matched;
            allMatched &= matched;
            if (kind == MatchKind.ALL && !allMatched || kind != MatchKind.ALL && anyMatched) {
                break;
            }
        }
        if (empty) {
            return ELOptional.empty();
        }
        return ELOptional.of(switch (kind) {
            case ANY -> anyMatched;
            case ALL -> allMatched;
            case NONE -> !anyMatched;
        });
    }

    @Nullable
    private Object invoke(LambdaExpression lambdaExpression, @Nullable Object... arguments) {
        return lambdaExpression.invoke(context, arguments);
    }

    private ELStream next(Stream<Object> nextStream) {
        return new ELStream(context, nextStream);
    }

    private static long longValue(@Nullable Object value) {
        Number number = ELSupport.coerceToNumber(value, long.class);
        return number == null ? 0L : number.longValue();
    }

    private static int intValue(@Nullable Object value) {
        Number number = ELSupport.coerceToNumber(value, int.class);
        return number == null ? 0 : number.intValue();
    }

    private enum MatchKind {
        ANY, ALL, NONE
    }
}
