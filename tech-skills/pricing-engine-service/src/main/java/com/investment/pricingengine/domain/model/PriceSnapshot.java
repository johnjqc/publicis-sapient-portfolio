package com.investment.pricingengine.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * Core domain aggregate for a single symbol's price state.
 * <p>
 * Designed for high-concurrency read/write:
 * - AtomicReference for the snapshot state (lock-free CAS updates)
 * - LongAdder for counting total updates (high-write, low-read contention)
 */
public final class PriceSnapshot {

    private final String symbol;
    private final AtomicReference<State> stateRef;
    private final LongAdder updateCount;

    private PriceSnapshot(String symbol, State initialState) {
        this.symbol = symbol;
        this.stateRef = new AtomicReference<>(initialState);
        this.updateCount = new LongAdder();
    }

    public static PriceSnapshot of(String symbol, BigDecimal firstPrice, Instant timestamp) {
        State initial = new State(
                firstPrice,
                firstPrice,         // averagePrice = firstPrice on first tick
                BigDecimal.ZERO,    // changePercent unknown yet
                BigDecimal.ZERO,    // volatility unknown yet
                firstPrice,         // previousClose = firstPrice on first tick
                timestamp,
                1L
        );
        return new PriceSnapshot(symbol, initial);
    }

    /**
     * Applies a new price tick using a lock-free CAS loop.
     * Recalculates average (running mean), percent change, and volatility (running stddev).
     */
    public void applyTick(BigDecimal newPrice, Instant timestamp) {
        State current;
        State updated;
        do {
            current = stateRef.get();
            updated = current.withNewTick(newPrice, timestamp);
        } while (!stateRef.compareAndSet(current, updated));

        updateCount.increment();
    }

    public String getSymbol() {
        return symbol;
    }

    public State getState() {
        return stateRef.get();
    }

    public long getTotalUpdates() {
        return updateCount.sum();
    }

}