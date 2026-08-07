package com.example.visittracker.repository;

import com.example.visittracker.entity.Visit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Component
public class MockVisitRepository {

    private final HashSet<Visit> visits = new HashSet<>();

    public boolean saveVisit(Visit visit) {
         if(visits.contains(visit)) {
             return false;
         }
         visits.add(visit);
         return true;
    }

    public List<Visit> getVisitsByDoctorId(Integer doctorId) {
        List<Visit> doctorVisits = new ArrayList<>();
        for (Visit v : visits) {
            if (v.getDoctor().getId().equals(doctorId)) {
                doctorVisits.add(v);
            }
        }

        return doctorVisits;
    }
}
