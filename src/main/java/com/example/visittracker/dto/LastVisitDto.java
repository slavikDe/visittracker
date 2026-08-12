package com.example.visittracker.dto;

public record LastVisitDto(
        String start,
        String end,
        DoctorSummaryDto doctor
) {
}
