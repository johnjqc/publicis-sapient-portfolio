package com.investment.marketsimulator.infrastructure.messaging.mapper;

import com.investment.marketsimulator.domain.event.MarketPriceUpdatedEvent;
import com.investment.marketsimulator.domain.model.MarketPrice;
import org.springframework.stereotype.Component;

/**
 * Converts the internal domain value (MarketPrice) into the event payload
 * published on Kafka (MarketPriceUpdatedEvent). Kept separate from the
 * publisher itself so the mapping rule has one obvious place to live.
 */
@Component
public class MarketPriceEventMapper {

    public MarketPriceUpdatedEvent toEvent(MarketPrice price) {
        return new MarketPriceUpdatedEvent(
                price.symbol().value(),
                price.price(),
                price.timestamp());
    }
}
