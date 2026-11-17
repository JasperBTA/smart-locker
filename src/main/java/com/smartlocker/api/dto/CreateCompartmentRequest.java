package com.smartlocker.api.dto;

import com.smartlocker.domain.CompartmentSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCompartmentRequest(
        @NotBlank(message = "Locker identifier must not be blank")
        @Size(max = 64, message = "Locker identifier must not exceed 64 characters")
        String lockerIdentifier,

        @NotBlank(message = "Compartment number must not be blank")
        @Size(max = 32, message = "Compartment number must not exceed 32 characters")
        String compartmentNumber,

        @NotNull(message = "Compartment size is required")
        CompartmentSize size
) {
}
