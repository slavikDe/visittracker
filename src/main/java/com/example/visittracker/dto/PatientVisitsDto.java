package com.example.visittracker.dto;

import java.util.List;

public record PatientVisitsDto(
        String firstName,
        String lastName,
        List<LastVisitDto> lastVisits
) {
}
