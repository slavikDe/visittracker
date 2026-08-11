package com.example.visittracker.controller;

import com.example.visittracker.dto.StatisticDto;
import com.example.visittracker.dto.DoctorDto;
import com.example.visittracker.dto.PatientDto;
import com.example.visittracker.dto.VisitDto;
import com.example.visittracker.entity.Visit;
import com.example.visittracker.service.VisitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
@RestController
public class TrackerController {

    private final VisitService visitService;

    @GetMapping
    public ResponseEntity<String> getPatientsVisits() {
        log.info("Received request to get patients visits");
        return ResponseEntity.ok().body("returning patients visits");
    }

    @PostMapping("/visit")
    public ResponseEntity<Visit> createVisit(@RequestBody VisitDto visitDto) {
        log.info("Received request to create visit: {}", visitDto);
        Visit visit = visitService.createVisit(visitDto);
        log.info("Created visit with id: {}", visit.getId());
        return ResponseEntity.ok().body(visit);
    }

    @PostMapping("/doctor")
    public ResponseEntity<Integer> createDoctor(@RequestBody DoctorDto doctorDto) {
        log.info("Received request to create doctor: {}", doctorDto);
        Integer doctorId = visitService.createDoctor(doctorDto);
        log.info("Created doctor with id: {}", doctorId);
        return ResponseEntity.ok().body(doctorId);
    }

    @PostMapping("/patient")
    public ResponseEntity<Integer> createPatient(@RequestBody PatientDto patientDto) {
        log.info("Received request to create patient: {}", patientDto);
        Integer patientId = visitService.createPatient(patientDto);
        log.info("Created patient with id: {}", patientId);
        return ResponseEntity.ok().body(patientId);
    }

    @GetMapping("/statistic")
    public ResponseEntity<StatisticDto> getStatistic() {
        log.info("Received request to get statistic");
        return ResponseEntity.ok(visitService.getStatistic());
    }

}
