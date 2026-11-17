package com.smartlocker.api.dto;

import com.smartlocker.domain.CompartmentSize;
import com.smartlocker.domain.CompartmentStatus;
import com.smartlocker.domain.LockerCompartment;

import java.time.Instant;
import java.util.UUID;

public record CompartmentResponse(
        UUID id,
        String lockerIdentifier,
        String compartmentNumber,
        CompartmentSize size,
        CompartmentStatus status,
        Instant allocationTimestamp,
        Long version
) {
    public static CompartmentResponse from(LockerCompartment compartment) {
        return new CompartmentResponse(
                compartment.getId(),
                compartment.getLockerIdentifier(),
                compartment.getCompartmentNumber(),
                compartment.getSize(),
                compartment.getStatus(),
                compartment.getCompartmentAllocationTimestamp(),
                compartment.getVersion()
        );
    }
}
