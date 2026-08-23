package io.micronaut.el.test;

import io.micronaut.core.annotation.Introspected;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A bean whose properties are primitives, so that the operators applied to them are inlined.
 */
@Introspected
public class Inventory {

    private final @Nullable String sku;
    private final int quantity;
    private final long reorderLevel;
    private final double price;
    private final float weight;
    private final boolean available;
    private final Suit suit;

    public Inventory(@Nullable String sku, int quantity, long reorderLevel, double price, float weight, boolean available, Suit suit) {
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

    public Object describe(Function<Inventory, Object> describer) {
        return describer.apply(this);
    }

    public double adjusted(PriceAdjuster adjuster) {
        return adjuster.adjust(price, quantity);
    }
}
