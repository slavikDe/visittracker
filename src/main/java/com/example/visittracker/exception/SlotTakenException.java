package com.example.visittracker.exception;

/**
 * Raised when a doctor already has a visit overlapping the requested time range.
 */
public class SlotTakenException extends RuntimeException {
    public SlotTakenException(String message) {
        super(message);
    }
}
