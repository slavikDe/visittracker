package com.example.visittracker.dto;

public record DoctorSummaryDto(
        String firstName,
        String lastName,
        long totalPatients
) {
}
