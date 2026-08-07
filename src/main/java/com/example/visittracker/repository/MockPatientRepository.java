package com.example.visittracker.repository;

import com.example.visittracker.entity.Doctor;
import com.example.visittracker.entity.Patient;
import com.example.visittracker.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MockPatientRepository {

    private final List<Patient> patients;

    public MockPatientRepository() {
        patients = new LinkedList<>();
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

}
