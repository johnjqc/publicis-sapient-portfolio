package com.investment.pricingengine.api.controller;

import com.investment.pricingengine.api.controller.dto.PriceSnapshotResponse;
import com.investment.pricingengine.api.mapper.PriceSnapshotApiMapper;
import com.investment.pricingengine.domain.port.in.QueryPriceSnapshotUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PriceSnapshotController implements PriceSnapshotApi {

    private final QueryPriceSnapshotUseCase queryPriceSnapshotUseCase;
    private final PriceSnapshotApiMapper mapper;

    @Override
    public ResponseEntity<List<PriceSnapshotResponse>> getAllPriceSnapshots() {

        List<PriceSnapshotResponse> response = queryPriceSnapshotUseCase.findAll()
                .stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<PriceSnapshotResponse> getPriceSnapshotBySymbol(String symbol) {

        return queryPriceSnapshotUseCase.findBySymbol(symbol.toUpperCase())
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}