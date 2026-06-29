package com.investment.pricingengine.domain;

import com.investment.pricingengine.domain.model.PriceSnapshot;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class PriceSnapshotConcurrencyTest {

    @Test
    void applyTick_concurrentUpdates_noUpdateLost() throws InterruptedException {
        // Given
        PriceSnapshot snapshot = PriceSnapshot.of("AAPL", BigDecimal.valueOf(100), Instant.now());
        int threads = 16;
        int ticksPerThread = 1000;
        int totalTicks = threads * ticksPerThread;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        // When — all threads hammer applyTick concurrently
        for (int t = 0; t < threads; t++) {
            final int base = t * ticksPerThread;
            pool.submit(() -> {
                try {
                    for (int i = 0; i < ticksPerThread; i++) {
                        snapshot.applyTick(BigDecimal.valueOf(100 + base + i), Instant.now());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        // Then — exactly totalTicks applied, no lost update
        // tickCount starts at 1 (from the initial of() call), so expect totalTicks + 1
        assertThat(snapshot.getState().tickCount()).isEqualTo(totalTicks + 1L);
        assertThat(snapshot.getTotalUpdates()).isEqualTo(totalTicks);
    }

    @Test
    void applyTick_singleThread_avgConverges() {
        // Given
        PriceSnapshot snapshot = PriceSnapshot.of("GOOG", BigDecimal.valueOf(100), Instant.now());

        // When — feed 99 more ticks at 200 → average should approach 200 over time
        for (int i = 0; i < 99; i++) {
            snapshot.applyTick(BigDecimal.valueOf(200), Instant.now());
        }

        BigDecimal avg = snapshot.getState().averagePrice();

        // Then — running average of 1×100 + 99×200 = (100 + 19800)/100 = 199
        assertThat(avg).isBetween(BigDecimal.valueOf(198), BigDecimal.valueOf(200));
    }

    @Test
    void applyTick_changePercent_calculatedRelativeToPreviousClose() {
        BigDecimal closePrice = BigDecimal.valueOf(100);
        PriceSnapshot snapshot = PriceSnapshot.of("TSLA", closePrice, Instant.now());

        // price goes to 110 → expected change = +10%
        snapshot.applyTick(BigDecimal.valueOf(110), Instant.now());

        BigDecimal change = snapshot.getState().changePercent();
        assertThat(change).isBetween(BigDecimal.valueOf(9.9), BigDecimal.valueOf(10.1));
    }
}