package com.investment.pricingengine.infrastructure.persistence.adapter;

import com.investment.pricingengine.domain.model.PriceSnapshot;
import com.investment.pricingengine.domain.port.out.PriceSnapshotStore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lock-free in-memory implementation of PriceSnapshotStore.
 * <p>
 * ConcurrentHashMap#computeIfAbsent guarantees that the factory lambda
 * is called at most once per key even under concurrent access — no external
 * synchronized block needed.
 * <p>
 * Lifecycle: single Spring singleton shared across all Kafka consumer threads.
 */
@Component
public class InMemoryPriceSnapshotStoreAdapter implements PriceSnapshotStore {

    private final ConcurrentHashMap<String, PriceSnapshot> store = new ConcurrentHashMap<>();

    @Override
    public Optional<PriceSnapshot> findBySymbol(String symbol) {

        return Optional.ofNullable(store.get(symbol));
    }

    @Override
    public PriceSnapshot getOrCreate(String symbol, BigDecimal initialPrice, Instant timestamp) {

        return store.computeIfAbsent(symbol, k -> PriceSnapshot.of(k, initialPrice, timestamp));
    }

    @Override
    public Collection<PriceSnapshot> findAll() {

        return store.values();
    }
}