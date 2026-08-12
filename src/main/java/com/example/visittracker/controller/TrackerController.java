package com.example.visittracker.controller;

import com.example.visittracker.dto.DoctorDto;
import com.example.visittracker.dto.PatientDto;
import com.example.visittracker.dto.PatientListResponse;
import com.example.visittracker.dto.VisitDto;
import com.example.visittracker.dto.VisitResponseDto;
import com.example.visittracker.service.PatientQueryService;
import com.example.visittracker.service.VisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
public class TrackerController {

    private final VisitService visitService;
    private final PatientQueryService patientQueryService;

    @GetMapping("/patients")
    public ResponseEntity<PatientListResponse> getPatients(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<Long> doctorIds) {
        log.info("Received request to get patients: page={}, size={}, search={}, doctorIds={}",
                page, size, search, doctorIds);
        return ResponseEntity.ok(patientQueryService.getPatients(page, size, search, doctorIds));
    }

    @PostMapping("/visit")
    public ResponseEntity<VisitResponseDto> createVisit(@RequestBody VisitDto visitDto) {
        log.info("Received request to create visit: {}", visitDto);
        VisitResponseDto visit = visitService.createVisit(visitDto);
        log.info("Created visit with id: {}", visit.id());
        return ResponseEntity.ok().body(visit);
    }

    @PostMapping("/doctor")
    public ResponseEntity<Long> createDoctor(@RequestBody DoctorDto doctorDto) {
        log.info("Received request to create doctor: {}", doctorDto);
        Long doctorId = visitService.createDoctor(doctorDto);
        log.info("Created doctor with id: {}", doctorId);
        return ResponseEntity.ok().body(doctorId);
    }

    @PostMapping("/patient")
    public ResponseEntity<Long> createPatient(@RequestBody PatientDto patientDto) {
        log.info("Received request to create patient: {}", patientDto);
        Long patientId = visitService.createPatient(patientDto);
        log.info("Created patient with id: {}", patientId);
        return ResponseEntity.ok().body(patientId);
    }
}
