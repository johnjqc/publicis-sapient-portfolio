package com.investment.marketsimulator.api;

import com.investment.marketsimulator.domain.model.Symbol;
import com.investment.marketsimulator.domain.port.in.MarketSimulationUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MarketSimulatorController implements MarketSimulatorApi {

    private final MarketSimulationUseCase simulationService;

    public MarketSimulatorController(MarketSimulationUseCase simulationService) {
        this.simulationService = simulationService;
    }

    @Override
    public ResponseEntity<List<String>> trackedSymbols() {

        List<String> symbols = simulationService.getTrackedSymbols()
                .stream()
                .map(Symbol::value)
                .toList();

        return ResponseEntity.ok(symbols);
    }
}
