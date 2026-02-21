CREATE TABLE orders (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_email VARCHAR(255) NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity     INTEGER      NOT NULL CHECK (quantity > 0),
    total_price  NUMERIC(12, 2) NOT NULL CHECK (total_price > 0),
    status       VARCHAR(20)  NOT NULL DEFAULT 'PLACED',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ
);

CREATE INDEX idx_orders_customer_email ON orders(customer_email);
CREATE INDEX idx_orders_status         ON orders(status);
