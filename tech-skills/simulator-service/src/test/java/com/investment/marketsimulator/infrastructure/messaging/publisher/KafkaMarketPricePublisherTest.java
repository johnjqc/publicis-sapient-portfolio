package com.investment.marketsimulator.infrastructure.messaging.publisher;

import com.investment.marketsimulator.domain.event.MarketPriceUpdatedEvent;
import com.investment.marketsimulator.domain.model.MarketPrice;
import com.investment.marketsimulator.domain.model.Symbol;
import com.investment.marketsimulator.domain.port.out.MarketPricePublisher;
import com.investment.marketsimulator.infrastructure.messaging.mapper.MarketPriceEventMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaMarketPricePublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, MarketPriceUpdatedEvent> kafkaTemplate = mock(KafkaTemplate.class);

    private MarketPricePublisher publisher;

    @BeforeEach
    void setUp() {
        when(kafkaTemplate.send(any(String.class), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        publisher = new KafkaMarketPricePublisher(kafkaTemplate, new MarketPriceEventMapper());
    }

    @Test
    void givenMarketPrice_whenPublish_thenSendsEventToKafkaUsingSymbolAsKey() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        MarketPrice price = MarketPrice.of(Symbol.of("AAPL"), new BigDecimal("201.35"), now);

        publisher.publish(price);

        ArgumentCaptor<MarketPriceUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(MarketPriceUpdatedEvent.class);

        verify(kafkaTemplate).send(
                eq(KafkaMarketPricePublisher.TOPIC),
                eq("AAPL"),
                eventCaptor.capture());

        MarketPriceUpdatedEvent event = eventCaptor.getValue();

        assertThat(event.symbol()).isEqualTo("AAPL");
        assertThat(event.price()).isEqualByComparingTo("201.35");
        assertThat(event.timestamp()).isEqualTo(now);
    }

}
