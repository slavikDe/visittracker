package com.example.visittracker.repository;

import com.example.visittracker.entity.Doctor;
import com.example.visittracker.entity.Patient;
import com.example.visittracker.exception.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.LinkedList;
import java.util.List;

@Component
public class MockDoctorRepository {

    private final List<Doctor> doctors;

    public MockDoctorRepository() {
        doctors = new LinkedList<>();
    }

    public Doctor getDoctorById(Integer id) {
        for (Doctor d : doctors) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        throw new NotFoundException("Doctor with id: " + id + " not found");
    }

    public void addDoctor(Doctor d) {
        if (d == null) throw new IllegalArgumentException("Cannot add new doctor, doctor can't be null");

        doctors.add(d);
    }

}
