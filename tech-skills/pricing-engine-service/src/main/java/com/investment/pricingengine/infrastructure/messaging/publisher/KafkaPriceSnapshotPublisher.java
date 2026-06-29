package com.investment.pricingengine.infrastructure.messaging.publisher;

import com.investment.pricingengine.domain.event.PriceSnapshotUpdated;
import com.investment.pricingengine.domain.port.out.PriceSnapshotEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka implementation of PriceSnapshotEventPublisher.
 * <p>
 * Uses the symbol as the partition key so all ticks for the same symbol
 * go to the same partition — preserving ordering for downstream consumers
 * (Portfolio Service).
 * <p>
 * Fire-and-forget with async callback for logging. For production,
 * consider a circuit breaker or a local buffer if Kafka is temporarily unavailable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPriceSnapshotPublisher implements PriceSnapshotEventPublisher {

    private final KafkaTemplate<String, PriceSnapshotUpdated> kafkaTemplate;

    @Value("${pricing-engine.topics.price-snapshot-updated}")
    private String topic;

    @Override
    public void publish(PriceSnapshotUpdated event) {
        CompletableFuture<SendResult<String, PriceSnapshotUpdated>> future =
                kafkaTemplate.send(topic, event.symbol(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[{}] Failed to publish PriceSnapshotUpdated for symbol={}: {}",
                        event.correlationId(), event.symbol(), ex.getMessage(), ex);
            } else {
                log.debug("[{}] Published PriceSnapshotUpdated for symbol={} partition={} offset={}",
                        event.correlationId(), event.symbol(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}