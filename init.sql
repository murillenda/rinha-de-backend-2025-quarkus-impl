DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS processor_status; -- Adicionado

CREATE UNLOGGED TABLE payments (
    correlationId UUID PRIMARY KEY,
    amount DECIMAL NOT NULL,
    requested_at TIMESTAMP NOT NULL
);
CREATE INDEX payments_requested_at ON payments (requested_at);

CREATE TABLE processor_status (
    processorName VARCHAR(10) PRIMARY KEY,
    isHealthy BOOLEAN NOT NULL,
    lastChecked TIMESTAMP NOT NULL
);
INSERT INTO processor_status (processorName, isHealthy, lastChecked) VALUES
    ('default', true, NOW()),
    ('fallback', true, NOW());