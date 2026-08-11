package com.example.visittracker.repository;

import com.example.visittracker.entity.Doctor;
import com.example.visittracker.entity.Patient;
import com.example.visittracker.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MockPatientRepository {

    private final HashSet<Patient> patients;

    public MockPatientRepository() {
        patients = new HashSet<>();
    }

    public Patient getPatientById(Integer id) {
        for (Patient p : patients) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        throw new NotFoundException("Patient with id: " + id + " not found");
    }

    public void addPatient(Patient p) {
        if (p == null) throw new IllegalArgumentException("Cannot add new patient, patient can't be null");

        patients.add(p);
    }

    public boolean exists(Patient patient) {
        return patients.contains(patient);
    }

    public Integer savePatient(Patient patient) {
        patients.add(patient);
        return patient.getId();
    }

    public List<Patient> getAllPatients() {
        return List.copyOf(patients);
    }
}
