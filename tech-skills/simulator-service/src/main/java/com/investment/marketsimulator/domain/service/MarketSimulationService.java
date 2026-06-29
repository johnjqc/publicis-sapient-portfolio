package com.investment.marketsimulator.domain.service;

import com.investment.marketsimulator.domain.model.MarketPrice;
import com.investment.marketsimulator.domain.model.Symbol;
import com.investment.marketsimulator.domain.model.TrackedInstrument;
import com.investment.marketsimulator.domain.port.in.MarketSimulationUseCase;
import com.investment.marketsimulator.domain.port.out.MarketPricePublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates a synthetic random-walk price for every tracked instrument and
 * publishes it through the outbound port.
 *
 * Uses a ConcurrentHashMap so instruments could later be added/removed at
 * runtime (e.g. via a future admin endpoint) without locking the whole map —
 * the same lock-free habit the roadmap calls for in the Pricing Engine.
 */
public class MarketSimulationService implements MarketSimulationUseCase {

    private final Map<Symbol, TrackedInstrument> instruments = new ConcurrentHashMap<>();
    private final MarketPricePublisher publisher;
    private final Clock clock;

    public MarketSimulationService(MarketPricePublisher publisher, Clock clock) {
        this.publisher = publisher;
        this.clock = clock;
    }

    public void track(TrackedInstrument instrument) {
        instruments.put(instrument.symbol(), instrument);
    }

    @Override
    public List<Symbol> getTrackedSymbols() {
        return List.copyOf(instruments.keySet());
    }

    @Override
    public void tickAll() {
        Instant now = clock.instant();
        for (TrackedInstrument instrument : instruments.values()) {
            var nextPrice = instrument.nextPrice();
            publisher.publish(MarketPrice.of(instrument.symbol(), nextPrice, now));
        }
    }
}
