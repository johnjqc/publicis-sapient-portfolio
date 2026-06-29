package com.investment.marketsimulator.domain.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The wire payload for a price update event, published on the
 * {@code market.price.updated} topic.
 * <p>
 * Deliberately a separate type from {@code MarketPrice}: this record is the
 * public contract other services consume, while MarketPrice is the internal
 * domain value. Keeping them distinct means changing internal representation
 * later doesn't silently change the event contract other services rely on.
 */
public record MarketPriceUpdatedEvent(String symbol, BigDecimal price, Instant timestamp) {
}
