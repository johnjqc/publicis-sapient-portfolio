package com.investment.marketsimulator.infrastructure.listener;

import com.investment.marketsimulator.domain.model.MarketPrice;
import com.investment.marketsimulator.domain.port.out.MarketPricePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Implements the MarketPricePublisher port by fanning out every price to a
 * list of in-process listeners.
 * <p>
 * CopyOnWriteArrayList because writes (subscribe) are rare and reads
 * (publish, called on every tick) are frequent and must never block.
 */
@Component
public class InMemoryMarketPricePublisher implements MarketPricePublisher {

    private static final Logger log = LoggerFactory.getLogger(InMemoryMarketPricePublisher.class);

    private final List<Consumer<MarketPrice>> listeners = new CopyOnWriteArrayList<>();

    public InMemoryMarketPricePublisher() {
        listeners.add(price -> log.info("{} -> {}", price.symbol(), price.price()));
    }

    @Override
    public void publish(MarketPrice price) {
        for (Consumer<MarketPrice> listener : listeners) {
            listener.accept(price);
        }
    }
}
