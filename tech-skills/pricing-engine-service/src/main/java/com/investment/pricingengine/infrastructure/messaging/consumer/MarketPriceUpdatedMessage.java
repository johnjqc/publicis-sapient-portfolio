package com.investment.pricingengine.infrastructure.messaging.consumer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Deserialization DTO for the MarketPriceUpdated Kafka event.
 * Shape must match whatever the Market Data Service / Market Simulator publishes.
 * <p>
 * Kept in infrastructure — the domain never sees this class.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarketPriceUpdatedMessage(
        @JsonProperty("correlationId") String correlationId,
        @JsonProperty("symbol")        String symbol,
        @JsonProperty("price")         BigDecimal price,
        @JsonProperty("timestamp")     Instant timestamp
) {}
