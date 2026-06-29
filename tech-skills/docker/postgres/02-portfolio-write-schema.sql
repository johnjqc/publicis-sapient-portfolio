\c portfolio_write_db;

-- Catálogo de portfolios
CREATE TABLE portfolio (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id      VARCHAR(64)    NOT NULL,
    name           VARCHAR(255)   NOT NULL,
    status         VARCHAR(32)    NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | SUSPENDED | CLOSED
    cash_balance   NUMERIC(20, 6) NOT NULL DEFAULT 0,
    currency       VARCHAR(3)     NOT NULL DEFAULT 'USD',
    version        BIGINT         NOT NULL DEFAULT 0,         -- optimistic locking
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_portfolio_client ON portfolio (client_id);

-- Posiciones individuales por símbolo
CREATE TABLE portfolio_position (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    portfolio_id        UUID           NOT NULL REFERENCES portfolio(id),
    symbol              VARCHAR(16)    NOT NULL,
    quantity            NUMERIC(20, 8) NOT NULL,
    average_cost        NUMERIC(20, 6) NOT NULL,         -- precio promedio de compra (Phase 3)
    current_price       NUMERIC(20, 6) NOT NULL DEFAULT 0,
    current_value       NUMERIC(20, 6) NOT NULL DEFAULT 0,  -- quantity × current_price
    last_price_update   TIMESTAMPTZ,
    version             BIGINT         NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_portfolio_symbol UNIQUE (portfolio_id, symbol)
);

CREATE INDEX idx_position_portfolio ON portfolio_position (portfolio_id);
CREATE INDEX idx_position_symbol    ON portfolio_position (symbol);  -- para actualizar por símbolo en bulk

-- Tabla de idempotencia: evita procesar el mismo PriceSnapshotUpdated dos veces
CREATE TABLE processed_event (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id       VARCHAR(128) NOT NULL UNIQUE,
    event_type     VARCHAR(64)  NOT NULL,
    processed_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- TTL manual: purgar eventos viejos con un job periódico
CREATE INDEX idx_processed_event_ts ON processed_event (processed_at);