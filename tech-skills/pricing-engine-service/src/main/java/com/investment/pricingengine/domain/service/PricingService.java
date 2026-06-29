package com.investment.pricingengine.domain.service;

import com.investment.pricingengine.domain.event.PriceSnapshotUpdated;
import com.investment.pricingengine.domain.model.PriceSnapshot;
import com.investment.pricingengine.domain.model.State;
import com.investment.pricingengine.domain.port.in.ProcessMarketPriceUseCase;
import com.investment.pricingengine.domain.port.in.QueryPriceSnapshotUseCase;
import com.investment.pricingengine.domain.port.out.PriceSnapshotEventPublisher;
import com.investment.pricingengine.domain.port.out.PriceSnapshotStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Central orchestrator for price processing.
 * <p>
 * Thread safety:
 * - PriceSnapshot.applyTick() is lock-free (CAS on AtomicReference).
 * - PriceSnapshotStore.getOrCreate() uses ConcurrentHashMap#computeIfAbsent — atomic.
 * - This class itself is stateless; all state lives in the store.
 * <p>
 * Therefore multiple Kafka consumer threads can call process() concurrently without
 * any synchronized block in this class.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingService implements ProcessMarketPriceUseCase, QueryPriceSnapshotUseCase {

    private final PriceSnapshotStore store;
    private final PriceSnapshotEventPublisher publisher;

    @Override
    public void process(String correlationId, String symbol, BigDecimal price, Instant timestamp) {
        log.info("[{}] Processing tick for {} @ {}", correlationId, symbol, price);

        // getOrCreate is atomic — safe under concurrent access
        PriceSnapshot snapshot = store.getOrCreate(symbol, price, timestamp);

        // CAS loop inside applyTick — no external lock needed
        snapshot.applyTick(price, timestamp);

        State state = snapshot.getState();

        PriceSnapshotUpdated event = new PriceSnapshotUpdated(
                correlationId,
                symbol,
                state.currentPrice(),
                state.averagePrice(),
                state.changePercent(),
                state.volatility(),
                state.tickCount(),
                timestamp
        );

        publisher.publish(event);

        log.info("[{}] Published snapshot for {} — price={} avg={} chg={}% vol={}",
                correlationId, symbol,
                state.currentPrice(), state.averagePrice(),
                state.changePercent(), state.volatility());
    }


    @Override
    public Optional<PriceSnapshot> findBySymbol(String symbol) {

        return store.findBySymbol(symbol);
    }

    @Override
    public List<PriceSnapshot> findAll() {

        return List.copyOf(store.findAll());
    }
}