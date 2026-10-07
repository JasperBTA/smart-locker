package com.smartlocker.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LockerCompartmentTest {

    private final UUID compartmentId = UUID.randomUUID();
    private final String lockerIdentifier = "Locker-1";
    private final String compartmentNumber = "C1";

    @Test
    @DisplayName("Initialize LockerCompartment with valid parameters should set status to AVAILABLE")
    void InitializeLockerCompartmentWithAvailableStatus() {
        LockerCompartment compartment = new LockerCompartment(
            compartmentId,
            lockerIdentifier,
            compartmentNumber,
            CompartmentSize.SMALL
        );

        assertThat(compartment.getStatus()).isEqualTo(CompartmentStatus.AVAILABLE);
        assertThat(compartment.getCompartmentAllocationTimestamp()).isNull();
    }

    @Test
    @DisplayName("Should transition from AVAILABLE to RESERVED on alocation")
    void ShouldTransitionFromAvailableToReservedOnAllocation() {
        LockerCompartment compartment = createAvailableCompartment();
        compartment.allocate();

        assertThat(compartment.getStatus()).isEqualTo(CompartmentStatus.RESERVED);
        assertThat(compartment.getCompartmentAllocationTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Should reject allocation when compartment is already occupied")
    void ShouldThrowExceptionWhenAllocatingOccupiedCompartment() {
        LockerCompartment compartment = createAvailableCompartment();
        compartment.allocate();
        compartment.occupy();

        assertThatThrownBy(compartment::allocate)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot allocate compartment in status: OCCUPIED");
    }

    @Test
    @DisplayName("Should transition from RESERVED to OCCUPIED when parcel is placed")
    void ShouldTransitionToOccupiedWhenParcelIsPlaced() {
        LockerCompartment compartment = createAvailableCompartment();
        compartment.allocate();
        compartment.occupy();

        assertThat(compartment.getStatus()).isEqualTo(CompartmentStatus.OCCUPIED);
    }

    @Test
    @DisplayName("Should transition to Available and clear timestamp when released")
    void ShouldTransitionToAvailableWhenReleased() {
        LockerCompartment compartment = createAvailableCompartment();
        compartment.allocate();
        compartment.occupy();
        compartment.release();

        assertThat(compartment.getStatus()).isEqualTo(CompartmentStatus.AVAILABLE);
        assertThat(compartment.getCompartmentAllocationTimestamp()).isNull();
    }

    @Test
    @DisplayName("Should reject marking out of service when compartment is occupied")
    void ShouldRejectMarkingOutOfServiceWhenOccupied() {
        LockerCompartment compartment = createAvailableCompartment();
        compartment.allocate();
        compartment.occupy();

        assertThatThrownBy(compartment::markOutOfService)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot mark occupied compartment as out of service");
    }

    private LockerCompartment createAvailableCompartment() {
        return new LockerCompartment(
            compartmentId,
            lockerIdentifier,
            compartmentNumber,
            CompartmentSize.SMALL
        );
    }

}