package com.investment.marketsimulator.domain.port.out;

import com.investment.marketsimulator.domain.model.MarketPrice;

/**
 * Outbound port: how the domain announces a new price to the outside world.
 * <p>
 * The domain depends only on this interface. Today it's implemented by an
 * in-memory publisher (infrastructure/listener). A KafkaMarketPricePublisher will implement this
 * same interface and the domain/service code will not change at all.
 */
public interface MarketPricePublisher {

    void publish(MarketPrice price);
}
