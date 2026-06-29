package com.investment.marketsimulator.infrastructure.config;

import com.investment.marketsimulator.domain.event.MarketPriceUpdatedEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the Kafka producer explicitly rather than relying only on
 * spring.kafka.* autoconfiguration, so the value type
 * (MarketPriceUpdatedEvent) and serializer are visible in one place instead
 * of split between Java config and properties.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    public ProducerFactory<String, MarketPriceUpdatedEvent> producerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, MarketPriceUpdatedEvent> kafkaTemplate(
            ProducerFactory<String, MarketPriceUpdatedEvent> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
