package com.smartlocker.exception;

import com.smartlocker.domain.CompartmentSize;

public class NoAvailableCompartmentException extends RuntimeException {

    public NoAvailableCompartmentException(String lockerIdentifier, CompartmentSize size) {
        super("No available compartment of size " + size + " in locker: " + lockerIdentifier);
    }
}
