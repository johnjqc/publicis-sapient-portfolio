package com.investment.marketsimulator.infrastructure.messaging.publisher;

import com.investment.marketsimulator.domain.model.MarketPrice;
import com.investment.marketsimulator.domain.model.Symbol;
import com.investment.marketsimulator.infrastructure.listener.InMemoryMarketPricePublisher;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CompositeMarketPricePublisherTest {

    @Test
    void givenCompositePublisher_whenPublish_thenDelegatesToInMemoryAndKafkaPublishers() {
        InMemoryMarketPricePublisher inMemoryPublisher = mock(InMemoryMarketPricePublisher.class);
        KafkaMarketPricePublisher kafkaPublisher = mock(KafkaMarketPricePublisher.class);
        CompositeMarketPricePublisher composite =
                new CompositeMarketPricePublisher(kafkaPublisher, inMemoryPublisher);

        MarketPrice price = MarketPrice.of(
                Symbol.of("AAPL"), new BigDecimal("201.35"), Instant.parse("2026-01-01T00:00:00Z"));

        composite.publish(price);

        verify(inMemoryPublisher).publish(price);
        verify(kafkaPublisher).publish(price);
    }
}
