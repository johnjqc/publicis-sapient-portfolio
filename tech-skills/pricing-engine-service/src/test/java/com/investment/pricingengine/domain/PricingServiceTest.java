package com.investment.pricingengine.domain;

import com.investment.pricingengine.domain.event.PriceSnapshotUpdated;
import com.investment.pricingengine.domain.model.PriceSnapshot;
import com.investment.pricingengine.domain.port.out.PriceSnapshotEventPublisher;
import com.investment.pricingengine.domain.port.out.PriceSnapshotStore;
import com.investment.pricingengine.domain.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PricingServiceTest {

    private PriceSnapshotStore store;
    private PriceSnapshotEventPublisher publisher;
    private PricingService service;

    @BeforeEach
    void setUp() {
        store     = mock(PriceSnapshotStore.class);
        publisher = mock(PriceSnapshotEventPublisher.class);
        service   = new PricingService(store, publisher);
    }

    @Test
    void process_publishesEventWithCorrectSymbol() {
        // Given
        String symbol = "AAPL";
        BigDecimal price = BigDecimal.valueOf(150);
        Instant now = Instant.now();
        PriceSnapshot snapshot = PriceSnapshot.of(symbol, price, now);

        when(store.getOrCreate(eq(symbol), eq(price), any())).thenReturn(snapshot);

        // When
        service.process("corr-123", symbol, price, now);

        // Then
        ArgumentCaptor<PriceSnapshotUpdated> captor = ArgumentCaptor.forClass(PriceSnapshotUpdated.class);
        verify(publisher).publish(captor.capture());

        PriceSnapshotUpdated event = captor.getValue();
        assertThat(event.symbol()).isEqualTo(symbol);
        assertThat(event.correlationId()).isEqualTo("corr-123");
        assertThat(event.currentPrice()).isEqualByComparingTo(price);
    }

    @Test
    void process_callsApplyTickOnExistingSnapshot() {
        // Given
        String symbol = "GOOG";
        Instant now = Instant.now();
        PriceSnapshot snapshot = spy(PriceSnapshot.of(symbol, BigDecimal.valueOf(100), now));

        when(store.getOrCreate(eq(symbol), any(), any())).thenReturn(snapshot);

        // When
        service.process("corr-456", symbol, BigDecimal.valueOf(105), now);

        // Then — applyTick was called once after getOrCreate
        verify(snapshot).applyTick(eq(BigDecimal.valueOf(105)), any());
    }

    @Test
    void findBySymbol_delegatesToStore() {
        // Given
        PriceSnapshot snap = PriceSnapshot.of("TSLA", BigDecimal.TEN, Instant.now());
        when(store.findBySymbol("TSLA")).thenReturn(Optional.of(snap));

        // When / Then
        assertThat(service.findBySymbol("TSLA")).contains(snap);
    }
}