CREATE TABLE orders (
    id                   uuid         PRIMARY KEY,
    order_number         varchar(30)  NOT NULL UNIQUE,
    user_id              uuid         NOT NULL, -- TODO(#TBD): users 테이블 반영 후 FK 추가
    status               varchar(30)  NOT NULL
        CHECK (status IN ('PENDING', 'PAID', 'FAILED', 'CANCELLED')),
    total_product_amount bigint       NOT NULL,
    shipping_fee         bigint       NOT NULL,
    final_amount         bigint       NOT NULL,
    recipient_name       varchar(100) NOT NULL,
    address_line1        varchar(500) NOT NULL,
    address_line2        varchar(500),
    postal_code          varchar(20),
    contact_type         varchar(20)
        CHECK (contact_type IN ('PHONE', 'KAKAO', 'INSTAGRAM', 'NONE')),
    contact_value        varchar(255),
    ordered_at           timestamptz  NOT NULL,
    created_at           timestamptz  NOT NULL,
    updated_at           timestamptz  NOT NULL
);

CREATE INDEX idx_orders_user_id ON orders (user_id);
CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE order_items (
    id           uuid         PRIMARY KEY,
    order_id     uuid         NOT NULL REFERENCES orders (id),
    product_id   uuid         NOT NULL REFERENCES products (id),
    product_name varchar(200) NOT NULL,
    unit_price   bigint       NOT NULL,
    quantity     int          NOT NULL CHECK (quantity > 0),
    created_at   timestamptz  NOT NULL,
    updated_at   timestamptz  NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE TABLE payments (
    id              uuid         PRIMARY KEY,
    order_id        uuid         NOT NULL REFERENCES orders (id),
    pg_provider     varchar(30)  NOT NULL CHECK (pg_provider IN ('TOSS')),
    payment_key     varchar(255),
    idempotency_key varchar(255) NOT NULL UNIQUE,
    method          varchar(30),
    amount          bigint       NOT NULL,
    status          varchar(20)  NOT NULL
        CHECK (status IN ('READY', 'DONE', 'FAILED', 'CANCELLED')),
    approved_at     timestamptz,
    failure_code    varchar(100),
    failure_message varchar(500),
    raw_response    jsonb,
    created_at      timestamptz  NOT NULL,
    updated_at      timestamptz  NOT NULL
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_payment_key ON payments (payment_key);
