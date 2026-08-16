package com.example.visittracker.repository.projection;

import java.time.Instant;

public interface LastVisitRow {

    Long getPatientId();

    Long getDoctorId();

    Instant getStartDateTime();

    Instant getEndDateTime();

    String getDoctorFirstName();

    String getDoctorLastName();

    String getDoctorTimezone();
}
