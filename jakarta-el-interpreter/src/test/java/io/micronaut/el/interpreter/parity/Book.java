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
package io.micronaut.el.interpreter.parity;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;

import java.util.List;
import java.util.function.Predicate;

/**
 * The interpreted twin of the {@code Book} beans of the Java, Groovy and Kotlin test suites, merged into one. The
 * expressions construct it as {@code Book('EL', 'history', 10)}, so its simple name cannot carry the
 * {@code Parity} prefix.
 */
@Introspected
public class Book {

    private String title;
    private final String category;
    private double unitPrice;

    public Book(String title, String category, double unitPrice) {
        this.title = title;
        this.category = category;
        this.unitPrice = unitPrice;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String describe() {
        return title + " (" + category + ")";
    }

    @Executable
    public double discounted(double percent) {
        return unitPrice * (1 - percent / 100);
    }

    public List<String> getTags() {
        return List.of("new", "sale", "b");
    }

    @Executable
    public long count(Predicate<String> predicate) {
        return getTags().stream().filter(predicate).count();
    }
}
