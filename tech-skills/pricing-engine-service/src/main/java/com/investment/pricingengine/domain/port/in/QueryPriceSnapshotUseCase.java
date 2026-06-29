package com.investment.pricingengine.domain.port.in;


import com.investment.pricingengine.domain.model.PriceSnapshot;

import java.util.List;
import java.util.Optional;

/**
 * Inbound port — queried by the REST controller.
 */
public interface QueryPriceSnapshotUseCase {

    Optional<PriceSnapshot> findBySymbol(String symbol);

    List<PriceSnapshot> findAll();
}
