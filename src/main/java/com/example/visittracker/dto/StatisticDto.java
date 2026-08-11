package com.example.visittracker.dto;

import java.util.List;

public record StatisticDto(
        List<VisitDto> visits,
        List<DoctorDto> doctors,
        List<PatientDto> patients
) {
}
