package com.smartlocker.service;

import com.smartlocker.domain.CompartmentSize;
import com.smartlocker.domain.CompartmentStatus;
import com.smartlocker.domain.LockerCompartment;
import com.smartlocker.exception.CompartmentNotFoundException;
import com.smartlocker.exception.DuplicateCompartmentException;
import com.smartlocker.exception.NoAvailableCompartmentException;
import com.smartlocker.repository.LockerCompartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class LockerCompartmentService {

    private final LockerCompartmentRepository repository;

    public LockerCompartmentService(LockerCompartmentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LockerCompartment createCompartment(String lockerIdentifier, String compartmentNumber, CompartmentSize size) {
        String normalizedLocker = lockerIdentifier.trim();
        String normalizedNumber = compartmentNumber.trim();

        if (repository.existsByLockerIdentifierAndCompartmentNumber(normalizedLocker, normalizedNumber)) {
            throw new DuplicateCompartmentException(normalizedLocker, normalizedNumber);
        }

        LockerCompartment compartment = new LockerCompartment(
                UUID.randomUUID(),
                normalizedLocker,
                normalizedNumber,
                size
        );
        return repository.saveAndFlush(compartment);
    }

    @Transactional(readOnly = true)
    public LockerCompartment getCompartment(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new CompartmentNotFoundException(id));
    }

    @Transactional
    public LockerCompartment allocateFirstAvailable(String lockerIdentifier, CompartmentSize size) {
        LockerCompartment compartment = repository
                .findFirstByLockerIdentifierAndSizeAndStatusOrderByCompartmentNumberAsc(
                        lockerIdentifier.trim(),
                        size,
                        CompartmentStatus.AVAILABLE
                )
                .orElseThrow(() -> new NoAvailableCompartmentException(lockerIdentifier.trim(), size));

        compartment.allocate();
        return repository.saveAndFlush(compartment);
    }

    @Transactional
    public LockerCompartment allocateCompartment(UUID id) {
        return transition(id, LockerCompartment::allocate);
    }

    @Transactional
    public LockerCompartment occupyCompartment(UUID id) {
        return transition(id, LockerCompartment::occupy);
    }

    @Transactional
    public LockerCompartment releaseCompartment(UUID id) {
        return transition(id, LockerCompartment::release);
    }

    @Transactional
    public LockerCompartment markOutOfService(UUID id) {
        return transition(id, LockerCompartment::markOutOfService);
    }

    @Transactional
    public LockerCompartment restoreToService(UUID id) {
        return transition(id, LockerCompartment::restoreToService);
    }

    @Transactional(readOnly = true)
    public List<LockerCompartment> getCompartmentsByLocker(String lockerIdentifier, CompartmentStatus status) {
        String normalizedLocker = lockerIdentifier.trim();
        if (status == null) {
            return repository.findAllByLockerIdentifierOrderByCompartmentNumberAsc(normalizedLocker);
        }
        return repository.findAllByLockerIdentifierAndStatusOrderByCompartmentNumberAsc(normalizedLocker, status);
    }

    @Transactional(readOnly = true)
    public LockerSummary getLockerSummary(String lockerIdentifier) {
        String normalizedLocker = lockerIdentifier.trim();
        List<LockerCompartment> compartments = repository
                .findAllByLockerIdentifierOrderByCompartmentNumberAsc(normalizedLocker);

        Map<CompartmentStatus, Long> counts = new EnumMap<>(CompartmentStatus.class);
        for (CompartmentStatus status : CompartmentStatus.values()) {
            counts.put(status, 0L);
        }
        for (LockerCompartment compartment : compartments) {
            counts.merge(compartment.getStatus(), 1L, Long::sum);
        }

        return new LockerSummary(
                normalizedLocker,
                compartments.size(),
                counts.get(CompartmentStatus.AVAILABLE),
                counts.get(CompartmentStatus.RESERVED),
                counts.get(CompartmentStatus.OCCUPIED),
                counts.get(CompartmentStatus.OUT_OF_SERVICE)
        );
    }

    private LockerCompartment transition(UUID id, Consumer<LockerCompartment> action) {
        LockerCompartment compartment = repository.findById(id)
                .orElseThrow(() -> new CompartmentNotFoundException(id));
        action.accept(compartment);
        return repository.saveAndFlush(compartment);
    }

    public record LockerSummary(
            String lockerIdentifier,
            long totalCompartments,
            long available,
            long reserved,
            long occupied,
            long outOfService
    ) {
    }
}
