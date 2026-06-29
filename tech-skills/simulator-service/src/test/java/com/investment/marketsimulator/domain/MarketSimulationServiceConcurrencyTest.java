package com.investment.marketsimulator.domain;

import com.investment.marketsimulator.domain.model.MarketPrice;
import com.investment.marketsimulator.domain.model.Symbol;
import com.investment.marketsimulator.domain.model.TrackedInstrument;
import com.investment.marketsimulator.domain.port.out.MarketPricePublisher;
import com.investment.marketsimulator.domain.service.MarketSimulationService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class MarketSimulationServiceConcurrencyTest {

    @Test
    void givenConcurrentTickAll_whenExecuted_thenPublishesAllPricesWithoutErrors() throws Exception {

        List<MarketPrice> published = Collections.synchronizedList(new ArrayList<>());

        MarketSimulationService service = new MarketSimulationService(
                published::add,
                Clock.systemUTC());

        service.track(TrackedInstrument.startingAt(
                Symbol.of("AAPL"),
                new BigDecimal("200"),
                15));

        service.track(TrackedInstrument.startingAt(
                Symbol.of("MSFT"),
                new BigDecimal("400"),
                12));

        int threads = 20;
        int iterationsPerThread = 100;

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch finish = new CountDownLatch(threads);

            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < iterationsPerThread; j++) {
                            service.tickAll();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finish.countDown();
                    }
                });
            }

            start.countDown();
            assertThat(finish.await(10, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
        }
        assertThat(published)
                .hasSize(threads * iterationsPerThread * 2);
    }

    @Test
    void givenConcurrentTrackAndTickAll_whenExecuted_thenCompletesWithoutErrors() throws Exception {

        MarketSimulationService service = new MarketSimulationService(
                price -> {},
                Clock.systemUTC());

        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            CountDownLatch start = new CountDownLatch(1);
            Future<?> trackTask = executor.submit(() -> {
                start.await();
                for (int i = 0; i < 500; i++) {
                    service.track(
                            TrackedInstrument.startingAt(
                                    Symbol.of("SYM" + i),
                                    BigDecimal.TEN,
                                    10));
                }
                return null;
            });

            Future<?> tickTask = executor.submit(() -> {
                start.await();
                for (int i = 0; i < 500; i++) {
                    service.tickAll();
                }
                return null;
            });

            start.countDown();
            trackTask.get();
            tickTask.get();
            executor.shutdown();
        }

        assertThat(service.getTrackedSymbols()).hasSize(500);
    }

    @Test
    void givenConcurrentNextPrice_whenExecuted_thenPriceIsAlwaysPositive() throws Exception {

        TrackedInstrument instrument =
                TrackedInstrument.startingAt(
                        Symbol.of("AAPL"),
                        new BigDecimal("200"),
                        20);

        int threads = 16;
        int iterations = 500;

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < iterations; j++) {
                        BigDecimal price = instrument.nextPrice();
                        assertThat(price)
                                .isGreaterThan(BigDecimal.ZERO);
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
            executor.shutdown();
        }
    }

    @Test
    void givenConcurrentReads_whenGettingTrackedSymbols_thenReturnsConsistentResults() throws Exception {

        MarketSimulationService service =
                new MarketSimulationService(price -> {}, Clock.systemUTC());

        for (int i = 0; i < 100; i++) {
            service.track(
                    TrackedInstrument.startingAt(
                            Symbol.of("SYM" + i),
                            BigDecimal.TEN,
                            10));
        }
        try (ExecutorService executor = Executors.newFixedThreadPool(20)) {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                futures.add(executor.submit(() -> {
                    for (int j = 0; j < 1000; j++) {
                        assertThat(service.getTrackedSymbols())
                                .hasSize(100);
                    }
                }));
            }
            for (Future<?> future : futures) {
                future.get();
            }
            executor.shutdown();
        }
    }

    @Test
    void givenConcurrentTickAllCalls_whenExecuted_thenPublishesOnePricePerSymbolWithoutErrors()
            throws Exception {

        List<MarketPrice> published =
                Collections.synchronizedList(new ArrayList<>());

        MarketPricePublisher publisher = published::add;

        Clock clock = Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"),
                ZoneOffset.UTC);

        MarketSimulationService service =
                new MarketSimulationService(publisher, clock);

        service.track(TrackedInstrument.startingAt(
                Symbol.of("AAPL"),
                new BigDecimal("200.00"),
                15));

        service.track(TrackedInstrument.startingAt(
                Symbol.of("MSFT"),
                new BigDecimal("400.00"),
                12));

        int threads = 20;
        int iterationsPerThread = 100;

        try (ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    for (int j = 0; j < iterationsPerThread; j++) {
                        service.tickAll();
                    }
                    return null;
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(published)
                .hasSize(threads * iterationsPerThread * 2);
    }

    @Test
    void givenConcurrentTrackTickAndRead_whenExecuted_thenCompletesWithoutErrors() throws Exception {

        List<MarketPrice> published =
                Collections.synchronizedList(new ArrayList<>());

        MarketSimulationService service =
                new MarketSimulationService(
                        published::add,
                        Clock.systemUTC());

        int symbols = 500;

        try (ExecutorService executor = Executors.newFixedThreadPool(12)) {

            CountDownLatch ready = new CountDownLatch(3);
            CountDownLatch start = new CountDownLatch(1);

            Future<?> trackFuture = executor.submit(() -> {

                ready.countDown();
                start.await();

                for (int i = 0; i < symbols; i++) {

                    service.track(
                            TrackedInstrument.startingAt(
                                    Symbol.of("SYM-" + i),
                                    BigDecimal.TEN,
                                    10));

                }

                return null;
            });

            Future<?> tickFuture = executor.submit(() -> {

                ready.countDown();
                start.await();

                for (int i = 0; i < symbols; i++) {
                    service.tickAll();
                }

                return null;
            });

            Future<?> readFuture = executor.submit(() -> {

                ready.countDown();
                start.await();

                for (int i = 0; i < symbols; i++) {
                    service.getTrackedSymbols();
                }

                return null;
            });

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            trackFuture.get();
            tickFuture.get();
            readFuture.get();

            executor.shutdown();

            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(service.getTrackedSymbols())
                .hasSize(symbols);
    }
}
