package com.example.visittracker.controller;

import com.example.visittracker.dto.DoctorDto;
import com.example.visittracker.entity.Visit;
import com.example.visittracker.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
public class TrackerController {

    private final VisitService visitService;

    @GetMapping
    public ResponseEntity<String> getPatientsVisits() {
        return ResponseEntity.ok().body("returning patients visits");
    }

    @PostMapping("/create")
    public ResponseEntity<Visit> createVisit(@RequestAttribute("start") String start,
                                              @RequestAttribute("end") String end,
                                              @RequestAttribute("patientId") Integer patientId,
                                              @RequestAttribute("doctorId") Integer doctorId
                                              ) {
        Visit visit = visitService.createVisit(start, end, patientId, doctorId);
        return ResponseEntity.ok().body(visit);
    }

    @PostMapping("/doctor/create")
    public ResponseEntity<Integer> createDoctor(@RequestBody DoctorDto doctor) {
        return ResponseEntity.ok().body(visitService.createDoctor(doctor));
    }
}
