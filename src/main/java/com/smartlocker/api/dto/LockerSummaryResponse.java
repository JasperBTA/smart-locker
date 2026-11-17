package com.smartlocker.api.dto;

import com.smartlocker.service.LockerCompartmentService.LockerSummary;

public record LockerSummaryResponse(
        String lockerIdentifier,
        long totalCompartments,
        long available,
        long reserved,
        long occupied,
        long outOfService
) {
    public static LockerSummaryResponse from(LockerSummary summary) {
        return new LockerSummaryResponse(
                summary.lockerIdentifier(),
                summary.totalCompartments(),
                summary.available(),
                summary.reserved(),
                summary.occupied(),
                summary.outOfService()
        );
    }
}
