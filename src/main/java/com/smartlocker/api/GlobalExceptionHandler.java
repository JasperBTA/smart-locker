package com.smartlocker.api;

import com.smartlocker.exception.CompartmentNotFoundException;
import com.smartlocker.exception.DuplicateCompartmentException;
import com.smartlocker.exception.NoAvailableCompartmentException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CompartmentNotFoundException.class)
    public ProblemDetail handleNotFound(CompartmentNotFoundException ex) {
        return buildProblemDetail(HttpStatus.NOT_FOUND, "Compartment Not Found", ex.getMessage());
    }

    @ExceptionHandler({
            NoAvailableCompartmentException.class,
            DuplicateCompartmentException.class,
            IllegalStateException.class
    })
    public ProblemDetail handleBusinessConflict(RuntimeException ex) {
        return buildProblemDetail(HttpStatus.CONFLICT, "Business Rule Conflict", ex.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockConflict(ObjectOptimisticLockingFailureException ex) {
        return buildProblemDetail(
                HttpStatus.CONFLICT,
                "Concurrent Modification Conflict",
                "Compartment state was concurrently modified by another transaction. Please retry."
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return buildProblemDetail(
                HttpStatus.CONFLICT,
                "Data Integrity Conflict",
                "A compartment with the given locker identifier and number already exists."
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (first, second) -> first
                ));

        ProblemDetail problem = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "Request validation failed for one or more fields."
        );
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail handleMalformedRequest(Exception ex) {
        return buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Malformed Request",
                "Request payload or parameter could not be parsed."
        );
    }

    private ProblemDetail buildProblemDetail(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
