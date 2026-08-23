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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The {@code Stream} implementation class described in the section 2.3.3.1 of the Jakarta Expression
 * Language specification.
 *
 * @param <T> The type of the elements, which the compiler knows from the source of the stream
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELStream<T> {

    private final ELContext context;
    private final Stream<T> stream;

    private ELStream(ELContext context, Stream<T> stream) {
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
    public static ELStream<Object> of(ELContext context, Object source) {
        if (source instanceof Collection<?> collection) {
            @SuppressWarnings("unchecked")
            Stream<Object> stream = (Stream<Object>) collection.stream();
            return new ELStream<>(context, stream);
        }
        if (source.getClass().isArray()) {
            int length = Array.getLength(source);
            List<Object> elements = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                elements.add(Array.get(source, i));
            }
            return new ELStream<>(context, elements.stream());
        }
        throw new ELException("Cannot create a stream from " + source.getClass().getName());
    }

    /**
     * @param predicate The predicate
     * @return A stream of the elements matching the predicate
     */
    public ELStream<T> filter(LambdaExpression predicate) {
        return filter(element -> ELSupport.toBoolean(invoke(predicate, element)));
    }

    /**
     * @param predicate The predicate, as compiled from a lambda expression
     * @return A stream of the elements matching the predicate
     */
    public ELStream<T> filter(Predicate<? super T> predicate) {
        return next(stream.filter(predicate));
    }

    /**
     * @param mapper The mapper
     * @return A stream of the mapped elements
     */
    public ELStream<Object> map(LambdaExpression mapper) {
        return map(element -> invoke(mapper, element));
    }

    /**
     * @param mapper The mapper, as compiled from a lambda expression
     * @param <R>    The type of the mapped elements
     * @return A stream of the mapped elements
     */
    public <R> ELStream<R> map(Function<? super T, ? extends R> mapper) {
        return next(stream.map(mapper));
    }

    /**
     * @param mapper The mapper producing a stream for every element
     * @return The concatenation of the mapped streams
     */
    public ELStream<Object> flatMap(LambdaExpression mapper) {
        return flatMap(element -> invoke(mapper, element));
    }

    /**
     * @param mapper The mapper producing a stream for every element, as compiled from a lambda expression
     * @param <R>    The type of the elements of the mapped streams
     * @return The concatenation of the mapped streams
     */
    @SuppressWarnings("unchecked")
    public <R> ELStream<R> flatMap(Function<? super T, ?> mapper) {
        return next(stream.flatMap(element -> {
            Object mapped = mapper.apply(element);
            if (mapped instanceof ELStream<?> elStream) {
                return (Stream<R>) elStream.stream;
            }
            throw new ELException("The flatMap function must return a stream");
        }));
    }

    /**
     * @return A stream of the distinct elements
     */
    public ELStream<T> distinct() {
        return next(stream.distinct());
    }

    /**
     * @return A stream of the elements sorted in their natural order
     */
    public ELStream<T> sorted() {
        return next(stream.sorted(ELSupport::compare));
    }

    /**
     * @param comparator The comparator
     * @return A stream of the elements sorted with the comparator
     */
    public ELStream<T> sorted(LambdaExpression comparator) {
        return sorted(comparator(comparator));
    }

    /**
     * @param comparator The comparator, as compiled from a lambda expression
     * @return A stream of the elements sorted with the comparator
     */
    public ELStream<T> sorted(Comparator<? super T> comparator) {
        return next(stream.sorted(comparator));
    }

    /**
     * @param consumer The consumer invoked for every element
     * @return Always {@code null}
     */
    @Nullable
    public Object forEach(LambdaExpression consumer) {
        return forEach(element -> invoke(consumer, element));
    }

    /**
     * @param consumer The consumer invoked for every element, as compiled from a lambda expression
     * @return Always {@code null}
     */
    @Nullable
    public Object forEach(Consumer<? super T> consumer) {
        stream.forEach(consumer);
        return null;
    }

    /**
     * @param consumer The consumer invoked for every element
     * @return A stream of the same elements
     */
    public ELStream<T> peek(LambdaExpression consumer) {
        return peek(element -> invoke(consumer, element));
    }

    /**
     * @param consumer The consumer invoked for every element, as compiled from a lambda expression
     * @return A stream of the same elements
     */
    public ELStream<T> peek(Consumer<? super T> consumer) {
        return next(stream.peek(consumer));
    }

    /**
     * @return An iterator over the elements of the stream
     */
    public Iterator<T> iterator() {
        return stream.iterator();
    }

    /**
     * @param count The maximum number of elements
     * @return A stream of at most {@code count} elements
     */
    public ELStream<T> limit(Object count) {
        return next(stream.limit(Math.max(0, longValue(count))));
    }

    /**
     * @param start The number of elements to skip
     * @return A stream skipping the first elements
     */
    public ELStream<T> substream(Object start) {
        return next(stream.skip(Math.max(0, longValue(start))));
    }

    /**
     * @param start The number of elements to skip
     * @param end   The exclusive end position
     * @return A stream of the elements between the two positions
     */
    public ELStream<T> substream(Object start, Object end) {
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
    public List<T> toList() {
        return stream.collect(java.util.stream.Collectors.toList());
    }

    /**
     * @param binaryOperator The accumulator
     * @return An optional holding the reduced value
     */
    public ELOptional<T> reduce(LambdaExpression binaryOperator) {
        return reduce(operator(binaryOperator));
    }

    /**
     * @param binaryOperator The accumulator, as compiled from a lambda expression
     * @return An optional holding the reduced value
     */
    public ELOptional<T> reduce(BinaryOperator<T> binaryOperator) {
        return ELOptional.of(stream.reduce(binaryOperator).orElse(null));
    }

    /**
     * @param seed           The seed
     * @param binaryOperator The accumulator
     * @return The reduced value
     */
    @Nullable
    public Object reduce(@Nullable Object seed, LambdaExpression binaryOperator) {
        return reduce(seed, (left, right) -> invoke(binaryOperator, left, right));
    }

    /**
     * @param seed           The seed
     * @param binaryOperator The accumulator, as compiled from a lambda expression
     * @return The reduced value
     */
    @Nullable
    public Object reduce(@Nullable Object seed, BinaryOperator<@Nullable Object> binaryOperator) {
        Object accumulator = seed;
        for (Iterator<T> iterator = stream.iterator(); iterator.hasNext();) {
            accumulator = binaryOperator.apply(accumulator, iterator.next());
        }
        return accumulator;
    }

    /**
     * @return An optional holding the maximum element
     */
    public ELOptional<T> max() {
        return ELOptional.of(stream.max(ELSupport::compare).orElse(null));
    }

    /**
     * @param comparator The comparator
     * @return An optional holding the maximum element
     */
    public ELOptional<T> max(LambdaExpression comparator) {
        return max(comparator(comparator));
    }

    /**
     * @param comparator The comparator, as compiled from a lambda expression
     * @return An optional holding the maximum element
     */
    public ELOptional<T> max(Comparator<? super T> comparator) {
        return ELOptional.of(stream.max(comparator).orElse(null));
    }

    /**
     * @return An optional holding the minimum element
     */
    public ELOptional<T> min() {
        return ELOptional.of(stream.min(ELSupport::compare).orElse(null));
    }

    /**
     * @param comparator The comparator
     * @return An optional holding the minimum element
     */
    public ELOptional<T> min(LambdaExpression comparator) {
        return min(comparator(comparator));
    }

    /**
     * @param comparator The comparator, as compiled from a lambda expression
     * @return An optional holding the minimum element
     */
    public ELOptional<T> min(Comparator<? super T> comparator) {
        return ELOptional.of(stream.min(comparator).orElse(null));
    }

    /**
     * @return An optional holding the average of the elements
     */
    public ELOptional<Object> average() {
        Object sum = Long.valueOf(0);
        long count = 0;
        for (Iterator<T> iterator = stream.iterator(); iterator.hasNext();) {
            sum = ELArithmetic.add(sum, iterator.next());
            count++;
        }
        if (count == 0) {
            return ELOptional.empty();
        }
        return ELOptional.of(ELArithmetic.divide(sum, count));
    }

    /**
     * @return The sum of the elements, zero for an empty stream
     */
    public Object sum() {
        Object sum = Long.valueOf(0);
        for (Iterator<T> iterator = stream.iterator(); iterator.hasNext();) {
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
    public ELOptional<Boolean> anyMatch(LambdaExpression predicate) {
        return anyMatch(predicate(predicate));
    }

    /**
     * @param predicate The predicate, as compiled from a lambda expression
     * @return The same as {@link #anyMatch(LambdaExpression)}
     */
    public ELOptional<Boolean> anyMatch(Predicate<? super T> predicate) {
        return match(predicate, MatchKind.ANY);
    }

    /**
     * @param predicate The predicate
     * @return An optional holding true when all the elements match, empty for an empty stream
     */
    public ELOptional<Boolean> allMatch(LambdaExpression predicate) {
        return allMatch(predicate(predicate));
    }

    /**
     * @param predicate The predicate, as compiled from a lambda expression
     * @return The same as {@link #allMatch(LambdaExpression)}
     */
    public ELOptional<Boolean> allMatch(Predicate<? super T> predicate) {
        return match(predicate, MatchKind.ALL);
    }

    /**
     * @param predicate The predicate
     * @return An optional holding true when no element matches, empty for an empty stream
     */
    public ELOptional<Boolean> noneMatch(LambdaExpression predicate) {
        return noneMatch(predicate(predicate));
    }

    /**
     * @param predicate The predicate, as compiled from a lambda expression
     * @return The same as {@link #noneMatch(LambdaExpression)}
     */
    public ELOptional<Boolean> noneMatch(Predicate<? super T> predicate) {
        return match(predicate, MatchKind.NONE);
    }

    /**
     * @return An optional holding the first element of the stream
     */
    public ELOptional<T> findFirst() {
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

    private ELOptional<Boolean> match(Predicate<? super T> predicate, MatchKind kind) {
        boolean empty = true;
        boolean anyMatched = false;
        boolean allMatched = true;
        for (Iterator<T> iterator = stream.iterator(); iterator.hasNext();) {
            empty = false;
            boolean matched = predicate.test(iterator.next());
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

    private Predicate<T> predicate(LambdaExpression predicate) {
        return element -> ELSupport.toBoolean(invoke(predicate, element));
    }

    private Comparator<T> comparator(LambdaExpression comparator) {
        return (left, right) -> intValue(invoke(comparator, left, right));
    }

    @SuppressWarnings("unchecked")
    private BinaryOperator<T> operator(LambdaExpression binaryOperator) {
        return (left, right) -> (T) invoke(binaryOperator, left, right);
    }

    private <R> ELStream<R> next(Stream<R> nextStream) {
        return new ELStream<>(context, nextStream);
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
