package com.investment.marketsimulator.domain.port.in;

import com.investment.marketsimulator.domain.model.Symbol;

import java.util.List;

/**
 * Inbound port: what the outside world (a scheduler, a REST endpoint, a CLI
 * command) can ask the simulator to do.
 */
public interface MarketSimulationUseCase {

    /**
     * Generates exactly one new price tick for every tracked symbol and
     * publishes each one through the configured MarketPricePublisher.
     */
    void tickAll();

    List<Symbol> getTrackedSymbols();
}
