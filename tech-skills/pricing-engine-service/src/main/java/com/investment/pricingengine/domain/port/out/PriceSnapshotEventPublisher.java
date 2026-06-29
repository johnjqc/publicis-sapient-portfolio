package com.investment.pricingengine.domain.port.out;

import com.investment.pricingengine.domain.event.PriceSnapshotUpdated;

/**
 * Outbound port — implemented by Kafka infrastructure layer.
 * The domain decides WHEN to publish; the adapter decides HOW.
 */
public interface PriceSnapshotEventPublisher {

    void publish(PriceSnapshotUpdated event);
}
