CREATE TABLE activation_requests (
    id UUID PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    product_code VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_activation_requests_external_id UNIQUE (external_id)
);

CREATE TABLE activation_outbox (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_outbox_request FOREIGN KEY (request_id) REFERENCES activation_requests(id) ON DELETE CASCADE
);

CREATE INDEX idx_activation_requests_cust_status ON activation_requests (customer_id, status);
CREATE INDEX idx_activation_requests_created_at ON activation_requests (created_at DESC);
CREATE INDEX idx_outbox_status_next_retry ON activation_outbox (status, next_retry_at);
