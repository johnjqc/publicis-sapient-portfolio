\c portfolio_read_db;

-- Proyección desnormalizada del portfolio completo (1 fila = 1 portfolio)
CREATE TABLE portfolio_summary_view (
    id               UUID PRIMARY KEY,
    client_id        VARCHAR(64)    NOT NULL,
    name             VARCHAR(255)   NOT NULL,
    status           VARCHAR(32)    NOT NULL,
    cash_balance     NUMERIC(20, 6) NOT NULL,
    positions_value  NUMERIC(20, 6) NOT NULL DEFAULT 0,  -- sum de current_value
    total_value      NUMERIC(20, 6) NOT NULL DEFAULT 0,  -- cash + positions_value
    total_pnl_pct    NUMERIC(10, 4) NOT NULL DEFAULT 0,
    currency         VARCHAR(3)     NOT NULL,
    position_count   INT            NOT NULL DEFAULT 0,
    last_updated     TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_summary_client ON portfolio_summary_view (client_id);

-- Proyección de posiciones lista para consumir directamente por el GET
CREATE TABLE position_view (
    id                  UUID PRIMARY KEY,
    portfolio_id        UUID           NOT NULL REFERENCES portfolio_summary_view(id),
    symbol              VARCHAR(16)    NOT NULL,
    quantity            NUMERIC(20, 8) NOT NULL,
    average_cost        NUMERIC(20, 6) NOT NULL,
    current_price       NUMERIC(20, 6) NOT NULL,
    current_value       NUMERIC(20, 6) NOT NULL,
    unrealized_pnl      NUMERIC(20, 6) NOT NULL DEFAULT 0,   -- current_value - (qty × avg_cost)
    unrealized_pnl_pct  NUMERIC(10, 4) NOT NULL DEFAULT 0,
    weight_pct          NUMERIC(10, 4) NOT NULL DEFAULT 0,   -- current_value / total_value × 100
    last_price_update   TIMESTAMPTZ,
    CONSTRAINT uq_view_portfolio_symbol UNIQUE (portfolio_id, symbol)
);

CREATE INDEX idx_position_view_portfolio ON position_view (portfolio_id);
CREATE INDEX idx_position_view_symbol    ON position_view (symbol);