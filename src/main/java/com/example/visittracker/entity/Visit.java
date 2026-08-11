package com.example.visittracker.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class Visit {
    private static int counter = 0;

    private Integer id;
    private Instant startDateTime;
    private Instant endDateTime;
    private Patient patient;
    private Doctor doctor;

    public Visit(Instant startDateTime, Instant endDateTime, Patient patient, Doctor doctor) {
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.patient = patient;
        this.doctor = doctor;
        id = ++counter;
    }
}
