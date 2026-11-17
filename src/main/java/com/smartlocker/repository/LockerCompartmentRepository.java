package com.smartlocker.repository;

import com.smartlocker.domain.CompartmentSize;
import com.smartlocker.domain.CompartmentStatus;
import com.smartlocker.domain.LockerCompartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LockerCompartmentRepository extends JpaRepository<LockerCompartment, UUID> {

    boolean existsByLockerIdentifierAndCompartmentNumber(String lockerIdentifier, String compartmentNumber);

    Optional<LockerCompartment> findFirstByLockerIdentifierAndSizeAndStatusOrderByCompartmentNumberAsc(
            String lockerIdentifier,
            CompartmentSize size,
            CompartmentStatus status
    );

    List<LockerCompartment> findAllByLockerIdentifierOrderByCompartmentNumberAsc(String lockerIdentifier);

    List<LockerCompartment> findAllByLockerIdentifierAndStatusOrderByCompartmentNumberAsc(
            String lockerIdentifier,
            CompartmentStatus status
    );
}
