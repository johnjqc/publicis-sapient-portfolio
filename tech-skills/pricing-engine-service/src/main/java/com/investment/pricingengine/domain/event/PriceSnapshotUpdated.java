package com.investment.pricingengine.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Domain event published after each tick recalculation.
 * Consumed by Portfolio Service.
 * <p>
 * Infrastructure layers are responsible for serialization.
 */
public record PriceSnapshotUpdated(
        String correlationId,
        String symbol,
        BigDecimal currentPrice,
        BigDecimal averagePrice,
        BigDecimal changePercent,
        BigDecimal volatility,
        long tickCount,
        Instant occurredAt
) {}