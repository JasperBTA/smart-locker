package com.smartlocker.exception;

import java.util.UUID;

public class CompartmentNotFoundException extends RuntimeException {

    public CompartmentNotFoundException(UUID id) {
        super("Compartment not found with ID: " + id);
    }
}
