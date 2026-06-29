package com.investment.marketsimulator.infrastructure.config;

import com.investment.marketsimulator.domain.model.Symbol;
import com.investment.marketsimulator.domain.model.TrackedInstrument;
import com.investment.marketsimulator.domain.port.out.MarketPricePublisher;
import com.investment.marketsimulator.domain.service.MarketSimulationService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/**
 * Wires the domain service together. This is the only place that knows both
 * the domain (MarketSimulationService) and a concrete port implementation
 * (whatever MarketPricePublisher bean Spring injects) — that's the adapter
 * boundary in action.
 */
@Configuration
public class MarketSimulatorConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public MarketSimulationService marketSimulationService(
            MarketPricePublisher publisher,
            Clock clock,
            SeedInstrumentsProperties seedProperties) {

        MarketSimulationService service = new MarketSimulationService(publisher, clock);
        seedProperties.toTrackedInstruments().forEach(service::track);

        return service;
    }

    @Bean
    @ConfigurationProperties(prefix = "market-simulator")
    public SeedInstrumentsProperties seedInstrumentsProperties() {

        return new SeedInstrumentsProperties();
    }

    /**
     * Binds the starting symbols/prices/volatility from application.yml so
     * the seed list isn't hardcoded in Java.
     * <p>
     * Deliberately a plain mutable class with JavaBean getters/setters, not a
     * record: Spring Boot's relaxed binding for nested list elements is most
     * reliable against no-arg-constructor + setter style. A record here would
     * need @ConstructorBinding wiring that adds risk for no real benefit in a
     * properties holder that's never used outside this configuration class.
     */
    @Setter
    @Getter
    public static class SeedInstrumentsProperties {

        private List<SeedInstrument> instruments = List.of(
                seedInstrument("AAPL", "201.35", 15.0),
                seedInstrument("MSFT", "468.12", 12.0),
                seedInstrument("NVDA", "180.52", 30.0)
        );

        private static SeedInstrument seedInstrument(String symbol, String price, double volatility) {
            return new SeedInstrument(symbol, new BigDecimal(price), volatility);
        }

        public List<TrackedInstrument> toTrackedInstruments() {
            return instruments.stream()
                    .map(s -> TrackedInstrument.startingAt(
                            Symbol.of(s.symbol()), s.startingPrice(), s.volatilityBasisPoints()))
                    .toList();
        }

        public  record SeedInstrument (
                String symbol,
                BigDecimal startingPrice,
                double volatilityBasisPoints) {}

    }
}
