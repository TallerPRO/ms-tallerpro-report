CREATE TABLE order_event (
    id              BIGSERIAL PRIMARY KEY,
    event_id        VARCHAR(100) NOT NULL UNIQUE,
    order_id        VARCHAR(60)  NOT NULL,
    taller_id       VARCHAR(60)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    event_timestamp TIMESTAMPTZ  NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_order_event_taller_status_ts
    ON order_event (taller_id, status, event_timestamp);

CREATE INDEX idx_order_event_order
    ON order_event (order_id);

CREATE TABLE order_state (
    order_id      VARCHAR(60) PRIMARY KEY,
    taller_id     VARCHAR(60)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    received_at   TIMESTAMPTZ,
    delivered_at  TIMESTAMPTZ,
    last_updated  TIMESTAMPTZ  NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_order_state_taller_status
    ON order_state (taller_id, status);
