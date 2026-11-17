CREATE TABLE locker_compartments (
    id UUID PRIMARY KEY,
    locker_identifier VARCHAR(64) NOT NULL,
    compartment_number VARCHAR(32) NOT NULL,
    size VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    allocation_timestamp TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_locker_compartment_number UNIQUE (locker_identifier, compartment_number)
);

CREATE INDEX idx_locker_compartments_lookup
    ON locker_compartments (locker_identifier, size, status);
