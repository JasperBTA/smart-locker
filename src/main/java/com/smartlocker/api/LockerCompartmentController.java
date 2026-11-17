package com.smartlocker.api;

import com.smartlocker.api.dto.AllocateCompartmentRequest;
import com.smartlocker.api.dto.CompartmentResponse;
import com.smartlocker.api.dto.CreateCompartmentRequest;
import com.smartlocker.api.dto.LockerSummaryResponse;
import com.smartlocker.domain.CompartmentStatus;
import com.smartlocker.domain.LockerCompartment;
import com.smartlocker.service.LockerCompartmentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class LockerCompartmentController {

    private final LockerCompartmentService service;

    public LockerCompartmentController(LockerCompartmentService service) {
        this.service = service;
    }

    @PostMapping("/compartments")
    public ResponseEntity<CompartmentResponse> createCompartment(@Valid @RequestBody CreateCompartmentRequest request) {
        LockerCompartment created = service.createCompartment(
                request.lockerIdentifier(),
                request.compartmentNumber(),
                request.size()
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(CompartmentResponse.from(created));
    }

    @GetMapping("/compartments/{id}")
    public ResponseEntity<CompartmentResponse> getCompartment(@PathVariable UUID id) {
        return ResponseEntity.ok(CompartmentResponse.from(service.getCompartment(id)));
    }

    @PostMapping("/lockers/{lockerIdentifier}/allocations")
    public ResponseEntity<CompartmentResponse> allocateFirstAvailable(
            @PathVariable String lockerIdentifier,
            @Valid @RequestBody AllocateCompartmentRequest request
    ) {
        LockerCompartment allocated = service.allocateFirstAvailable(lockerIdentifier, request.size());
        return ResponseEntity.ok(CompartmentResponse.from(allocated));
    }

    @PostMapping("/compartments/{id}/allocate")
    public ResponseEntity<CompartmentResponse> allocateCompartment(@PathVariable UUID id) {
        return ResponseEntity.ok(CompartmentResponse.from(service.allocateCompartment(id)));
    }

    @PostMapping("/compartments/{id}/occupy")
    public ResponseEntity<CompartmentResponse> occupyCompartment(@PathVariable UUID id) {
        return ResponseEntity.ok(CompartmentResponse.from(service.occupyCompartment(id)));
    }

    @PostMapping("/compartments/{id}/release")
    public ResponseEntity<CompartmentResponse> releaseCompartment(@PathVariable UUID id) {
        return ResponseEntity.ok(CompartmentResponse.from(service.releaseCompartment(id)));
    }

    @PostMapping("/compartments/{id}/out-of-service")
    public ResponseEntity<CompartmentResponse> markOutOfService(@PathVariable UUID id) {
        return ResponseEntity.ok(CompartmentResponse.from(service.markOutOfService(id)));
    }

    @PostMapping("/compartments/{id}/restore")
    public ResponseEntity<CompartmentResponse> restoreToService(@PathVariable UUID id) {
        return ResponseEntity.ok(CompartmentResponse.from(service.restoreToService(id)));
    }

    @GetMapping("/lockers/{lockerIdentifier}/compartments")
    public ResponseEntity<List<CompartmentResponse>> getCompartmentsByLocker(
            @PathVariable String lockerIdentifier,
            @RequestParam(required = false) CompartmentStatus status
    ) {
        List<CompartmentResponse> response = service.getCompartmentsByLocker(lockerIdentifier, status).stream()
                .map(CompartmentResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lockers/{lockerIdentifier}/summary")
    public ResponseEntity<LockerSummaryResponse> getLockerSummary(@PathVariable String lockerIdentifier) {
        return ResponseEntity.ok(LockerSummaryResponse.from(service.getLockerSummary(lockerIdentifier)));
    }
}
