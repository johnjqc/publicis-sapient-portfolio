package com.investment.pricingengine.infrastructure.messaging.consumer;

import com.investment.pricingengine.domain.port.in.ProcessMarketPriceUseCase;
import com.investment.pricingengine.infrastructure.messaging.mapper.MarketPriceMessageMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for the market-price-updated topic.
 * <p>
 * Concurrency: the number of concurrent listener threads is controlled by
 * `spring.kafka.listener.concurrency` (or the `concurrency` attribute below).
 * Each thread processes messages from one partition at a time — Kafka's ordering
 * guarantee is maintained per partition.
 * <p>
 * Idempotency note: Phase 2 does not add a deduplication key store, but the
 * architecture is ready for it (add a Set<String> processedIds in infrastructure).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceUpdatedConsumer {

    private final ProcessMarketPriceUseCase processMarketPriceUseCase;
    private final MarketPriceMessageMapper mapper;
    private final MeterRegistry meterRegistry;

    @KafkaListener(
            topics       = "${pricing-engine.topics.market-price-updated}",
            groupId      = "${spring.kafka.consumer.group-id}",
            concurrency  = "${pricing-engine.concurrency.consumer-threads}"
    )
    public void onMessage(
            ConsumerRecord<String, MarketPriceUpdatedMessage> record,
            Acknowledgment ack
    ) {
        MarketPriceUpdatedMessage msg = record.value();

        if (msg == null) {
            log.warn("Received null message at offset={} partition={}", record.offset(), record.partition());
            ack.acknowledge();
            return;
        }

        // Propagate correlationId to MDC for structured logging
        String correlationId = msg.correlationId() != null ? msg.correlationId() : "unknown";
        MDC.put("correlationId", correlationId);

        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            log.debug("Received tick symbol={} price={} offset={}", msg.symbol(), msg.price(), record.offset());

            processMarketPriceUseCase.process(
                    correlationId,
                    msg.symbol(),
                    msg.price(),
                    msg.timestamp()
            );

            ack.acknowledge();

            sample.stop(meterRegistry.timer("pricing.tick.processed",
                    "symbol", msg.symbol()));

        } catch (Exception ex) {
            log.error("[{}] Failed processing tick for symbol={}: {}", correlationId, msg.symbol(), ex.getMessage(), ex);
            // Don't ack — let Kafka retry (or send to DLQ if configured)
            throw ex;
        } finally {
            MDC.remove("correlationId");
        }
    }
}