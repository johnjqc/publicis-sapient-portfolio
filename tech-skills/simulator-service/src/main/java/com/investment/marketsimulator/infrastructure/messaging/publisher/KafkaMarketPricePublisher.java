package com.investment.marketsimulator.infrastructure.messaging.publisher;

import com.investment.marketsimulator.domain.event.MarketPriceUpdatedEvent;
import com.investment.marketsimulator.domain.model.MarketPrice;
import com.investment.marketsimulator.domain.port.out.MarketPricePublisher;
import com.investment.marketsimulator.infrastructure.messaging.mapper.MarketPriceEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Implements the MarketPricePublisher outbound port using Kafka.
 * <p>
 * Partition key is the symbol (e.g. "AAPL"), not the whole event: this
 * guarantees every price update for a given instrument lands in the same
 * partition and is therefore delivered to consumers in the order it was
 * produced. Without this, a consumer scaled across partitions could see
 * AAPL's price go 201 -> 203 -> 202 out of order, which would corrupt any
 * downstream volatility.
 * <p>
 * Fire-and-forget is intentional here: a single dropped price tick on a
 * synthetic feed is not a correctness problem the way a dropped trade event
 * would be.
 * The failure callback only logs, so a broker hiccup never crashes the
 * scheduler thread driving the simulation.
 */
@Component
public class KafkaMarketPricePublisher implements MarketPricePublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaMarketPricePublisher.class);

    public static final String TOPIC = "market.price.updated";

    private final KafkaTemplate<String, MarketPriceUpdatedEvent> kafkaTemplate;
    private final MarketPriceEventMapper mapper;

    public KafkaMarketPricePublisher(
            KafkaTemplate<String, MarketPriceUpdatedEvent> kafkaTemplate,
            MarketPriceEventMapper mapper) {

        this.kafkaTemplate = kafkaTemplate;
        this.mapper = mapper;
    }

    @Override
    public void publish(MarketPrice price) {
        String key = price.symbol().value();
        MarketPriceUpdatedEvent event = mapper.toEvent(price);

        kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, exception) -> {

                    if (exception != null) {
                        log.warn("Failed to publish price update for {} to topic {}", key, TOPIC, exception);
                    }
                });
    }
}
