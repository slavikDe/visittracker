package com.example.visittracker.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Resource not found", e.getMessage(), "not-found");
    }

    @ExceptionHandler(SlotTakenException.class)
    public ProblemDetail handleSlotTaken(SlotTakenException e) {
        return problem(HttpStatus.CONFLICT, "Time slot already taken", e.getMessage(), "slot-taken");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleValidation(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage(), "validation");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body",
                "Request body is missing or is not valid JSON", "validation");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error",
                "Something went wrong, please try again later", "internal");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String type) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        body.setType(URI.create("https://api.visittracker.example/errors/" + type));
        body.setProperty("timestamp", Instant.now());
        return body;
    }
}
