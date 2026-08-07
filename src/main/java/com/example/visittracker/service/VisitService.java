package com.example.visittracker.service;

import com.example.visittracker.dto.DoctorDto;
import com.example.visittracker.entity.Doctor;
import com.example.visittracker.entity.Patient;
import com.example.visittracker.entity.Visit;
import com.example.visittracker.exception.NotFoundException;
import com.example.visittracker.repository.MockDoctorRepository;
import com.example.visittracker.repository.MockPatientRepository;
import com.example.visittracker.repository.MockVisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@RequiredArgsConstructor
@Service
public class VisitService {
    private final MockPatientRepository patientRepository;
    private final MockVisitRepository visitRepository;
    private final MockDoctorRepository doctorRepository;

    public List<Visit> getVisitsByDoctorId(Integer doctorId) {
        doctorRepository.getDoctorById(doctorId);

        List<Visit> visits = visitRepository.getVisitsByDoctorId(doctorId);
        if (visits.isEmpty()) {
            throw new NotFoundException("Not found visits for doctor with id:" + doctorId);
        }

        return visits;
    }

    public Visit createVisit(String start, String end, Integer patientId, Integer doctorId) {
        if (patientId == null) throw new IllegalArgumentException("Patient id can't be null");
        if (doctorId == null) throw new IllegalArgumentException("Doctor id can't be null");

        Instant startDateTime = parseDateTime(start, "start");
        Instant endDateTime = parseDateTime(end, "end");

        if (!startDateTime.isBefore(endDateTime)) {
            throw new IllegalArgumentException("Visit start must be before its end");
        }

        Doctor doc = doctorRepository.getDoctorById(doctorId);
        Patient pat = patientRepository.getPatientById(patientId);

        Visit visit = new Visit(startDateTime, endDateTime, pat, doc);
        if (!visitRepository.saveVisit(visit)) {
            throw new IllegalArgumentException("Such visit already exists");
        }

        return visit;
    }

    public Integer createDoctor(DoctorDto dto) {
        Doctor doctor = new Doctor(Math.ra)
        return 1;
    }


    private Instant parseDateTime(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Visit " + field + " can't be empty");
        }

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Visit " + field + " must be an ISO-8601 date time, e.g. 2026-08-06T10:00:00Z, but was: " + value);
        }
    }
}
