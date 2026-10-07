package com.smartlocker.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class LockerCompartment {

    private final UUID id;
    private final String lockerIdentifier;
    private final String compartmentNumber;
    private final CompartmentSize size;
    private CompartmentStatus status;
    private Instant compartmentAllocationTimestamp;
    private Long version;

    public LockerCompartment(UUID id, String lockerIdentifier, String compartmentNumber, CompartmentSize size) {
        this.id = Objects.requireNonNull(id, "Compartment ID cannot be null");
        this.lockerIdentifier = Objects.requireNonNull(lockerIdentifier, "Locker identifier cannot be null");
        this.compartmentNumber = Objects.requireNonNull(compartmentNumber, "Compartment number cannot be null");
        this.size = Objects.requireNonNull(size, "Compartment size cannot be null");
        this.status = CompartmentStatus.AVAILABLE;
    }

    public void allocate() {
        if (this.status != CompartmentStatus.AVAILABLE) {
            throw new IllegalStateException("Cannot allocate compartment in status: " + this.status);
        }
        this.status = CompartmentStatus.RESERVED;
        this.compartmentAllocationTimestamp = Instant.now();
    }

    public void occupy() {
        if (this.status != CompartmentStatus.RESERVED) {
            throw new IllegalStateException("Cannot occupy compartment in status: " + this.status);
        }
        this.status = CompartmentStatus.OCCUPIED;
    }

    public void release() {
        if (this.status != CompartmentStatus.OCCUPIED && this.status != CompartmentStatus.RESERVED) {
            throw new IllegalStateException("Cannot release compartment in status: " + this.status);
        }
        this.status = CompartmentStatus.AVAILABLE;
        this.compartmentAllocationTimestamp = null;
    }

    public void markOutOfService() {
        if (this.status == CompartmentStatus.OCCUPIED) {
            throw new IllegalStateException("Cannot mark occupied compartment as out of service");
        }
        this.status = CompartmentStatus.OUT_OF_SERVICE;
    }

    public UUID getId() {
        return id;
    }

    public String getLockerIdentifier() {
        return lockerIdentifier;
    }

    public String getCompartmentNumber() {
        return compartmentNumber;
    }

    public CompartmentSize getSize() {
        return size;
    }

    public CompartmentStatus getStatus() {
        return status;
    }

    public Instant getCompartmentAllocationTimestamp() {
        return compartmentAllocationTimestamp;
    }

    public Long getVersion() {
        return version;
    }
}