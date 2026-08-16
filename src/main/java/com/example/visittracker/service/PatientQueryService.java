package com.example.visittracker.service;

import com.example.visittracker.dto.DoctorSummaryDto;
import com.example.visittracker.dto.LastVisitDto;
import com.example.visittracker.dto.PatientListResponse;
import com.example.visittracker.dto.PatientVisitsDto;
import com.example.visittracker.entity.Patient;
import com.example.visittracker.repository.PatientRepository;
import com.example.visittracker.repository.VisitRepository;
import com.example.visittracker.repository.projection.DoctorPatientCount;
import com.example.visittracker.repository.projection.LastVisitRow;
import com.example.visittracker.validation.DateValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PatientQueryService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private static final List<Long> NO_DOCTOR_FILTER = List.of(-1L);

    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;

    @Transactional(readOnly = true)
    public PatientListResponse getPatients(Integer page, Integer size, String search, List<Long> doctorIds) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;

        if (pageNumber < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page number can't be negative, but was: " + pageNumber);
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page size must be between 1 and " + MAX_PAGE_SIZE + ", but was: " + pageSize);
        }

        boolean filterByDoctor = doctorIds != null && !doctorIds.isEmpty();
        Collection<Long> doctorFilter = filterByDoctor ? doctorIds : NO_DOCTOR_FILTER;
        String searchPrefix = search == null ? "" : search.trim();

        Page<Patient> patientPage = patientRepository.findPage(
                searchPrefix, filterByDoctor ? 1 : 0, doctorFilter, PageRequest.of(pageNumber, pageSize));

        List<Patient> patients = patientPage.getContent();
        if (patients.isEmpty()) {
            return new PatientListResponse(List.of(), patientPage.getTotalElements());
        }

        List<Long> patientIds = patients.stream().map(Patient::getId).toList();

        List<LastVisitRow> rows = visitRepository.findLastVisitPerDoctor(
                patientIds, filterByDoctor ? 1 : 0, doctorFilter);

        Map<Long, Long> totalPatientsByDoctor = countPatientsPerDoctor(rows);
        Map<Long, List<LastVisitDto>> visitsByPatient = groupVisitsByPatient(rows, totalPatientsByDoctor);

        List<PatientVisitsDto> data = patients.stream()
                .map(p -> new PatientVisitsDto(
                        p.getFirstName(),
                        p.getLastName(),
                        visitsByPatient.getOrDefault(p.getId(), List.of())))
                .toList();

        return new PatientListResponse(data, patientPage.getTotalElements());
    }

    private Map<Long, Long> countPatientsPerDoctor(List<LastVisitRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }

        Set<Long> doctorIds = new HashSet<>();
        for (LastVisitRow row : rows) {
            doctorIds.add(row.getDoctorId());
        }

        Map<Long, Long> totals = new HashMap<>();
        for (DoctorPatientCount count : visitRepository.countDistinctPatientsPerDoctor(doctorIds)) {
            totals.put(count.getDoctorId(), count.getTotalPatients());
        }
        return totals;
    }

    private Map<Long, List<LastVisitDto>> groupVisitsByPatient(List<LastVisitRow> rows,
                                                               Map<Long, Long> totalPatientsByDoctor) {
        Map<Long, List<LastVisitDto>> visitsByPatient = new HashMap<>();

        for (LastVisitRow row : rows) {
            ZoneId doctorZone = ZoneId.of(row.getDoctorTimezone());

            LastVisitDto visit = new LastVisitDto(
                    DateValidator.format(row.getStartDateTime(), doctorZone),
                    DateValidator.format(row.getEndDateTime(), doctorZone),
                    new DoctorSummaryDto(
                            row.getDoctorFirstName(),
                            row.getDoctorLastName(),
                            totalPatientsByDoctor.getOrDefault(row.getDoctorId(), 0L)));

            visitsByPatient.computeIfAbsent(row.getPatientId(), id -> new ArrayList<>()).add(visit);
        }

        return visitsByPatient;
    }
}
