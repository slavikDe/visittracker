package com.example.visittracker.repository;

import com.example.visittracker.entity.Visit;
import com.example.visittracker.repository.projection.DoctorPatientCount;
import com.example.visittracker.repository.projection.LastVisitRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    @Query("""
            SELECT COUNT(v) > 0
            FROM Visit v
            WHERE v.doctor.id = :doctorId
              AND v.startDateTime < :end
              AND :start < v.endDateTime
            """)
    boolean existsOverlapping(@Param("doctorId") Long doctorId,
                              @Param("start") Instant start,
                              @Param("end") Instant end);

    @Query(value = """
            SELECT t.patient_id           AS patientId,
                   t.doctor_id            AS doctorId,
                   t.start_date_time      AS startDateTime,
                   t.end_date_time        AS endDateTime,
                   d.first_name           AS doctorFirstName,
                   d.last_name            AS doctorLastName,
                   d.timezone             AS doctorTimezone
            FROM (SELECT v.patient_id,
                         v.doctor_id,
                         v.start_date_time,
                         v.end_date_time,
                         ROW_NUMBER() OVER (PARTITION BY v.patient_id, v.doctor_id
                                            ORDER BY v.start_date_time DESC) AS rn
                  FROM visits v
                  WHERE v.patient_id IN (:patientIds)
                    AND (:filterByDoctor = 0 OR v.doctor_id IN (:doctorIds))) t
                     JOIN doctors d ON d.id = t.doctor_id
            WHERE t.rn = 1
            ORDER BY t.patient_id, t.start_date_time DESC
            """, nativeQuery = true)
    List<LastVisitRow> findLastVisitPerDoctor(@Param("patientIds") Collection<Long> patientIds,
                                              @Param("filterByDoctor") int filterByDoctor,
                                              @Param("doctorIds") Collection<Long> doctorIds);

    @Query(value = """
            SELECT v.doctor_id               AS doctorId,
                   COUNT(DISTINCT v.patient_id) AS totalPatients
            FROM visits v
            WHERE v.doctor_id IN (:doctorIds)
            GROUP BY v.doctor_id
            """, nativeQuery = true)
    List<DoctorPatientCount> countDistinctPatientsPerDoctor(@Param("doctorIds") Collection<Long> doctorIds);

}
