package com.example.visittracker.repository;

import com.example.visittracker.entity.TimeRange;
import com.example.visittracker.entity.Visit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MockVisitRepository {

    /**
     * Visits bucketed by doctor id. Stands in for the {@code (doctor_id, start_date_time)} index the
     * real table will need: every lookup here touches one doctor's visits, never the whole set.
     */
    private final Map<Integer, List<Visit>> visitsByDoctor = new ConcurrentHashMap<>();

    public Visit saveVisit(Visit visit) {
        visitsByDoctor
                .computeIfAbsent(visit.getDoctor().getId(), id -> Collections.synchronizedList(new ArrayList<>()))
                .add(visit);
        return visit;
    }

    /**
     * Equivalent of:
     * <pre>
     * SELECT 1 FROM visits
     *  WHERE doctor_id = ? AND start_date_time &lt; ? AND ? &lt; end_date_time
     *  LIMIT 1
     * </pre>
     * Short-circuits on the first conflict instead of materialising the doctor's whole schedule.
     */
    public boolean existsOverlappingVisit(Integer doctorId, TimeRange range) {
        List<Visit> doctorVisits = visitsByDoctor.get(doctorId);
        if (doctorVisits == null) {
            return false;
        }

        synchronized (doctorVisits) {
            for (Visit v : doctorVisits) {
                if (new TimeRange(v.getStartDateTime(), v.getEndDateTime()).overlaps(range)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Visit> getVisitsByDoctorId(Integer doctorId) {
        List<Visit> doctorVisits = visitsByDoctor.get(doctorId);
        if (doctorVisits == null) {
            return List.of();
        }

        synchronized (doctorVisits) {
            return List.copyOf(doctorVisits);
        }
    }

    public List<Visit> getAllVisits() {
        List<Visit> all = new ArrayList<>();
        for (List<Visit> doctorVisits : visitsByDoctor.values()) {
            synchronized (doctorVisits) {
                all.addAll(doctorVisits);
            }
        }
        return all;
    }
}
