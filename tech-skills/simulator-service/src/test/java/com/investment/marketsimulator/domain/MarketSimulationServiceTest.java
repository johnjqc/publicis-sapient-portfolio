package com.investment.marketsimulator.domain;

import com.investment.marketsimulator.domain.model.MarketPrice;
import com.investment.marketsimulator.domain.model.Symbol;
import com.investment.marketsimulator.domain.model.TrackedInstrument;
import com.investment.marketsimulator.domain.port.out.MarketPricePublisher;
import com.investment.marketsimulator.domain.service.MarketSimulationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketSimulationServiceTest {

    @Test
    void givenMultipleTrackedInstruments_whenTickAll_thenPublishesMarketPriceForEachInstrument() {
        List<MarketPrice> published = new ArrayList<>();
        MarketPricePublisher recordingPublisher = published::add;
        Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

        MarketSimulationService service = new MarketSimulationService(recordingPublisher, fixedClock);
        service.track(TrackedInstrument.startingAt(Symbol.of("AAPL"), new BigDecimal("200.00"), 15.0));
        service.track(TrackedInstrument.startingAt(Symbol.of("MSFT"), new BigDecimal("400.00"), 12.0));

        service.tickAll();

        assertThat(published).hasSize(2);
        assertThat(published).extracting(p -> p.symbol().value())
                .containsExactlyInAnyOrder("AAPL", "MSFT");
    }

    @Test
    void givenInstrumentWithExtremeVolatility_whenNextPrice_thenGeneratedPriceNeverBecomesZeroOrNegative() {
        TrackedInstrument instrument = TrackedInstrument.startingAt(
                Symbol.of("AAPL"), new BigDecimal("0.02"), 500.0);

        for (int i = 0; i < 1000; i++) {
            BigDecimal price = instrument.nextPrice();
            assertThat(price).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Test
    void givenTrackedInstruments_whenRetrievingTrackedSymbols_thenReturnsEveryTrackedSymbol() {
        MarketSimulationService service = new MarketSimulationService(price -> { }, Clock.systemUTC());
        service.track(TrackedInstrument.startingAt(Symbol.of("AAPL"), new BigDecimal("200.00"), 15.0));
        service.track(TrackedInstrument.startingAt(Symbol.of("NVDA"), new BigDecimal("180.00"), 30.0));

        assertThat(service.getTrackedSymbols())
                .extracting(Symbol::value)
                .containsExactlyInAnyOrder("AAPL", "NVDA");
    }
}
