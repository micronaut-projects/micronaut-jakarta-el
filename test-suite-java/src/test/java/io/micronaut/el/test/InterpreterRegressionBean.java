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
package io.micronaut.el.test;

import java.util.List;
import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

/**
 * The bean of the interpreter regressions, the compile-time twin of {@code InterpreterRegressionTest.Bean}.
 */
public final class InterpreterRegressionBean {

    /**
     * The number of elements {@link #parallelMap(Mapper)} maps concurrently.
     */
    public static final int ELEMENTS = 200_000;

    public String getName() {
        return "n";
    }

    public int two() {
        return 2;
    }

    public String iu(IntUnaryOperator f) {
        return "" + f.andThen(x -> x + 1).applyAsInt(1);
    }

    public String op(Operator f) {
        return "" + f.twice(1);
    }

    public List<Object> parallelMap(Mapper mapper) {
        return IntStream.range(0, ELEMENTS).parallel().mapToObj(mapper::map).toList();
    }

    /**
     * An interface of the application with a default method.
     */
    public interface Operator {

        int apply(int value);

        default int twice(int value) {
            return apply(value) + apply(value) + 1;
        }
    }

    /**
     * An interface of the application.
     */
    public interface Mapper {

        Object map(int value);
    }
}
