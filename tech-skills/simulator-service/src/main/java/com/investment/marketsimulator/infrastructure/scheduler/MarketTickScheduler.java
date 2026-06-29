package com.investment.marketsimulator.infrastructure.scheduler;

import com.investment.marketsimulator.domain.port.in.MarketSimulationUseCase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives the simulation on a fixed interval using Spring's scheduler.
 * <p>
 * This is infrastructure, not domain: it knows about @Scheduled, the domain
 * does not. It only calls the inbound port (MarketSimulationUseCase), so
 * swapping Spring's scheduler for something else later (e.g. a dedicated
 * thread, a virtual-thread-per-tick model) only touches this class.
 */
@Component
public class MarketTickScheduler {

    private final MarketSimulationUseCase simulationUseCase;

    public MarketTickScheduler(MarketSimulationUseCase simulationUseCase) {
        this.simulationUseCase = simulationUseCase;
    }

    @Scheduled(fixedRateString = "${market-simulator.tick-interval-ms:1000}")
    public void tick() {
        simulationUseCase.tickAll();
    }
}
