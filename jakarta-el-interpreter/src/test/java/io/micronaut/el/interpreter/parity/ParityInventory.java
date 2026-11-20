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

import io.micronaut.core.annotation.Introspected;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The interpreted twin of {@code io.micronaut.el.test.Inventory}, whose primitive properties have their operators
 * inlined by the compiler.
 */
@Introspected
public class ParityInventory {

    private final @Nullable String sku;
    private final int quantity;
    private final long reorderLevel;
    private final double price;
    private final float weight;
    private final boolean available;
    private final Suit suit;

    public ParityInventory(@Nullable String sku, int quantity, long reorderLevel, double price, float weight,
                           boolean available, Suit suit) {
        this.sku = sku;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
        this.price = price;
        this.weight = weight;
        this.available = available;
        this.suit = suit;
    }

    @Nullable
    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getReorderLevel() {
        return reorderLevel;
    }

    public double getPrice() {
        return price;
    }

    public float getWeight() {
        return weight;
    }

    public boolean isAvailable() {
        return available;
    }

    public Suit getSuit() {
        return suit;
    }

    public double getNan() {
        return Double.NaN;
    }

    public List<String> getTags() {
        return List.of("new", "sale", "b");
    }

    public Optional<String> getNote() {
        return Optional.ofNullable(sku);
    }

    public long count(Predicate<String> predicate) {
        return getTags().stream().filter(predicate).count();
    }

    public Object describe(Function<ParityInventory, Object> describer) {
        return describer.apply(this);
    }

    public double adjusted(ParityPriceAdjuster adjuster) {
        return adjuster.adjust(price, quantity);
    }
}
