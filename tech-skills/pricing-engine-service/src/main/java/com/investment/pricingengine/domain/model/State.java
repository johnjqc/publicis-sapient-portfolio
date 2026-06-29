package com.investment.pricingengine.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;

public record State(
        BigDecimal currentPrice,
        BigDecimal averagePrice,
        BigDecimal changePercent,
        BigDecimal volatility,
        BigDecimal previousClose,
        Instant lastUpdated,
        long tickCount
) {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);

    /**
     * Returns a new State derived from this one by incorporating newPrice.
     * Uses Welford's online algorithm for numerically stable running mean and variance.
     */
    State withNewTick(BigDecimal newPrice, Instant timestamp) {
        long n = tickCount + 1;

        // Running mean (Welford step 1): mean_n = mean_(n-1) + (x - mean_(n-1)) / n
        BigDecimal delta = newPrice.subtract(averagePrice);
        BigDecimal newAverage = averagePrice.add(delta.divide(BigDecimal.valueOf(n), MC));

        // Percent change from previousClose
        BigDecimal newChangePercent = previousClose.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : newPrice.subtract(previousClose)
                  .divide(previousClose, MC)
                  .multiply(BigDecimal.valueOf(100));

        // Simplified running volatility proxy: |changePercent| EMA-like decay
        BigDecimal newVolatility = volatility
                .multiply(BigDecimal.valueOf(0.9))
                .add(newChangePercent.abs().multiply(BigDecimal.valueOf(0.1)));

        return new State(newPrice, newAverage, newChangePercent, newVolatility, previousClose, timestamp, n);
    }
}
