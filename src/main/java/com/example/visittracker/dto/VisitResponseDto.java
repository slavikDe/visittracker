package com.example.visittracker.dto;

public record VisitResponseDto(
        Long id,
        String start,
        String end,
        Long patientId,
        Long doctorId
) {
}
