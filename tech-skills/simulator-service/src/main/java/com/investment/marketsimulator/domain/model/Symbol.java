package com.investment.marketsimulator.domain.model;

/**
 * A tradable instrument ticker (e.g. "AAPL", "MSFT").
 * Modeled as a value object so an invalid ticker can never enter the domain.
 */
public record Symbol(String value) {

    public Symbol {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Symbol value must not be blank");
        }
        value = value.toUpperCase();
    }

    public static Symbol of(String value) {
        return new Symbol(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
