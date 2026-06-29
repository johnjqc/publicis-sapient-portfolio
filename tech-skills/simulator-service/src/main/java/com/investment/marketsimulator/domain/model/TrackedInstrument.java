package com.investment.marketsimulator.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

public final class TrackedInstrument {

    private static final BigDecimal MIN_PRICE = BigDecimal.valueOf(0.01);

    private final Symbol symbol;
    private volatile BigDecimal lastPrice;
    private final double volatilityBasisPoints;

    private TrackedInstrument(Symbol symbol, BigDecimal startingPrice, double volatilityBasisPoints) {
        this.symbol = symbol;
        this.lastPrice = startingPrice;
        this.volatilityBasisPoints = volatilityBasisPoints;
    }

    public static TrackedInstrument startingAt(
            Symbol symbol, BigDecimal startingPrice, double volatilityBasisPoints) {

        return new TrackedInstrument(symbol, startingPrice, volatilityBasisPoints);
    }

    public synchronized BigDecimal nextPrice() {
        double drift = ThreadLocalRandom.current().nextGaussian() * volatilityBasisPoints / 10_000.0;
        BigDecimal candidate = lastPrice.add(lastPrice.multiply(BigDecimal.valueOf(drift)))
                .setScale(2, RoundingMode.HALF_UP);

        if (candidate.compareTo(MIN_PRICE) < 0) {
            candidate = MIN_PRICE;
        }

        this.lastPrice = candidate;
        return candidate;
    }

    public Symbol symbol() {

        return symbol;
    }
}
