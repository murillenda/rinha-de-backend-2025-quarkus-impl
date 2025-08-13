DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS processor_status;

CREATE TABLE processor_status (
    processorName VARCHAR(10) PRIMARY KEY,
    isHealthy BOOLEAN NOT NULL,
    lastChecked TIMESTAMP NOT NULL
);

INSERT INTO processor_status (processorName, isHealthy, lastChecked) VALUES
    ('default', true, NOW()),
    ('fallback', true, NOW());

CREATE UNLOGGED TABLE payments (
    correlationId UUID PRIMARY KEY,
    amount DECIMAL NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    status VARCHAR(10) NOT NULL,
    processor VARCHAR(10)
);

CREATE INDEX idx_payments_status ON payments (status);