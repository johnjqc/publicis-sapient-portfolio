package com.investment.pricingengine.api.mapper;

import com.investment.pricingengine.api.controller.dto.PriceSnapshotResponse;
import com.investment.pricingengine.domain.model.PriceSnapshot;
import com.investment.pricingengine.domain.model.State;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class PriceSnapshotApiMapper {

    public PriceSnapshotResponse toResponse(PriceSnapshot snapshot) {
        State state = snapshot.getState();
        return new PriceSnapshotResponse(
                snapshot.getSymbol(),
                state.currentPrice(),
                state.averagePrice(),
                state.changePercent(),
                state.volatility(),
                state.tickCount(),
                snapshot.getTotalUpdates(),
                OffsetDateTime.ofInstant(state.lastUpdated(), ZoneOffset.UTC)
        );
    }
}
