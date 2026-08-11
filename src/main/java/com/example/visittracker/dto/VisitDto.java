package com.example.visittracker.dto;

public record VisitDto (
        String start,
        String end,
        Integer patientId,
        Integer doctorId
) {
}
