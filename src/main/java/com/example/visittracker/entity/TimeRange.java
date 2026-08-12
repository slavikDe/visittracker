package com.example.visittracker.entity;

import java.time.Instant;

public record TimeRange(Instant start, Instant end) {

    public TimeRange {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Visit start and end can't be null");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Visit start must be before its end");
        }
    }

    public boolean overlaps(TimeRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
