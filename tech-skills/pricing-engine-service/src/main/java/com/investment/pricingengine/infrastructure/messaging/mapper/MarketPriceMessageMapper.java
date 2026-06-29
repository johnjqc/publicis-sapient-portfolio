package com.investment.pricingengine.infrastructure.messaging.mapper;

import com.investment.pricingengine.infrastructure.messaging.consumer.MarketPriceUpdatedMessage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Maps between the Kafka message DTO and domain primitives.
 */
@Component
public class MarketPriceMessageMapper {

    public String symbol(MarketPriceUpdatedMessage msg) {
        return msg.symbol();
    }

    public BigDecimal price(MarketPriceUpdatedMessage msg) {

        return msg.price();
    }

    public Instant timestamp(MarketPriceUpdatedMessage msg) {
        return msg.timestamp() != null ? msg.timestamp() : Instant.now();
    }
}
