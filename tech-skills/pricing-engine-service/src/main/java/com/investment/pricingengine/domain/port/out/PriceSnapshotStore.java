package com.investment.pricingengine.domain.port.out;

import com.investment.pricingengine.domain.model.PriceSnapshot;

import java.util.Collection;
import java.util.Optional;

/**
 * Outbound port for reading and writing PriceSnapshot state.
 * <p>
 * This is backed by a ConcurrentHashMap in infrastructure.
 */
public interface PriceSnapshotStore {

    /**
     * Retrieves an existing snapshot, or empty if this symbol hasn't been seen yet.
     */
    Optional<PriceSnapshot> findBySymbol(String symbol);

    /**
     * Atomically retrieves an existing snapshot or creates and stores a new one
     * with the given initial price. Equivalent to ConcurrentHashMap#computeIfAbsent.
     */
    PriceSnapshot getOrCreate(String symbol, java.math.BigDecimal initialPrice, java.time.Instant timestamp);

    Collection<PriceSnapshot> findAll();
}
