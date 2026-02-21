CREATE TABLE notifications (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID        NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    type            VARCHAR(30) NOT NULL,
    status          VARCHAR(10) NOT NULL,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_order_id ON notifications(order_id);
CREATE INDEX idx_notifications_status   ON notifications(status);
