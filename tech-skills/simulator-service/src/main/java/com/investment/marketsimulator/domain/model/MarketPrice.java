package com.investment.marketsimulator.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single price observation for a symbol at a point in time.
 * <p>
 * Immutable on purpose: every tick is a new value, never a mutation of a
 * previous one. This is what allows the rest of the system (Pricing Engine,
 * later) to treat ticks as an event stream instead of shared mutable state.
 */
public record MarketPrice(Symbol symbol, BigDecimal price, Instant timestamp) {

    public MarketPrice {
        if (symbol == null) {
            throw new IllegalArgumentException("symbol must not be null");
        }
        if (price == null || price.signum() <= 0) {
            throw new IllegalArgumentException("price must be a positive value");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
    }

    public static MarketPrice of(Symbol symbol, BigDecimal price, Instant timestamp) {
        return new MarketPrice(symbol, price, timestamp);
    }
}
