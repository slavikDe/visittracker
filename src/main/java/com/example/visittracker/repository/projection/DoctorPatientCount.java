package com.example.visittracker.repository.projection;

/**
 * Number of distinct patients that have visited a doctor at least once.
 */
public interface DoctorPatientCount {

    Long getDoctorId();

    long getTotalPatients();
}
