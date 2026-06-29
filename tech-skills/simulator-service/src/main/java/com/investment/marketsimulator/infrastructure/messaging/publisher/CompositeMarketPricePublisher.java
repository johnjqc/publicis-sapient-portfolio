package com.investment.marketsimulator.infrastructure.messaging.publisher;

import com.investment.marketsimulator.domain.model.MarketPrice;
import com.investment.marketsimulator.domain.port.out.MarketPricePublisher;
import com.investment.marketsimulator.infrastructure.listener.InMemoryMarketPricePublisher;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Fans out every price update to both the Kafka publisher and the in-memory
 * publisher, while still satisfying a single MarketPricePublisher port.
 *
 * This is the adapter that lets local development (the in-memory listener,
 * useful for quick log inspection or for tests with no broker running)
 * coexist with the real Kafka publish path. The domain layer never sees this
 * class — it only depends on the MarketPricePublisher interface.
 *
 * @Primary is required because InMemoryMarketPricePublisher and
 * KafkaMarketPricePublisher are themselves Spring beans implementing the
 * same interface (each still useful on its own — e.g. for a future
 * Kafka-only or in-memory-only profile). Without @Primary, Spring would have
 * three ambiguous candidates wherever MarketPricePublisher is injected.
 *
 * If either publisher fails, the other one isn't affected: a Kafka outage
 * doesn't prevent local listeners from still seeing ticks, and an issue in a
 * local listener doesn't block the Kafka publish.
 */
@Primary
@Component
public class CompositeMarketPricePublisher implements MarketPricePublisher {

    private final KafkaMarketPricePublisher kafkaPublisher;
    private final InMemoryMarketPricePublisher inMemoryPublisher;

    public CompositeMarketPricePublisher(
            KafkaMarketPricePublisher kafkaPublisher,
            InMemoryMarketPricePublisher inMemoryPublisher) {

        this.kafkaPublisher = kafkaPublisher;
        this.inMemoryPublisher = inMemoryPublisher;
    }

    @Override
    public void publish(MarketPrice price) {
        inMemoryPublisher.publish(price);
        kafkaPublisher.publish(price);
    }
}
