package com.example.visittracker.repository;

import com.example.visittracker.entity.Doctor;
import com.example.visittracker.exception.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

@Component
public class MockDoctorRepository {

    private final HashSet<Doctor> doctors;

    public MockDoctorRepository() {
        doctors = new HashSet<>();
    }

    public Doctor getDoctorById(Integer id) {
        for (Doctor d : doctors) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        throw new NotFoundException("Doctor with id: " + id + " not found");
    }

    public Integer saveDoctor(Doctor d) {
        if (d == null) throw new IllegalArgumentException("Cannot add new doctor, doctor can't be null");

        doctors.add(d);
        return d.getId();
    }

    public boolean exists(Doctor doctor) {
        return doctors.contains(doctor);
    }

    public List<Doctor> getAllDoctors() {
        return List.copyOf(doctors);
    }
}
