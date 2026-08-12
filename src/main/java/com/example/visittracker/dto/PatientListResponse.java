package com.example.visittracker.dto;

import java.util.List;

public record PatientListResponse(
        List<PatientVisitsDto> data,
        long count
) {
}
