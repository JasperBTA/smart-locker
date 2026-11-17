package com.smartlocker.api.dto;

import com.smartlocker.domain.CompartmentSize;
import jakarta.validation.constraints.NotNull;

public record AllocateCompartmentRequest(
        @NotNull(message = "Compartment size is required")
        CompartmentSize size
) {
}
