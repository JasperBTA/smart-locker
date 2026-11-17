package com.smartlocker.exception;

public class DuplicateCompartmentException extends RuntimeException {

    public DuplicateCompartmentException(String lockerIdentifier, String compartmentNumber) {
        super("Compartment " + compartmentNumber + " already exists in locker: " + lockerIdentifier);
    }
}
