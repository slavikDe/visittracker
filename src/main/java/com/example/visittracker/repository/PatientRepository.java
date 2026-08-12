package com.example.visittracker.repository;

import com.example.visittracker.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * One page of patients matching the optional name prefix and doctor filter.
     * <p>
     * Both filters are switched off with sentinels rather than {@code IS NULL} checks: an empty
     * {@code :search} and {@code :filterByDoctor = 0} let MySQL short-circuit the predicate. The
     * {@code doctorIds} list is never empty (callers pass a sentinel id), because {@code IN ()} is
     * a syntax error in SQL.
     * <p>
     * Spring Data runs this plus {@code countQuery} — two round trips for the whole page.
     */
    @Query(value = """
            SELECT p.id, p.first_name, p.last_name
            FROM patients p
            WHERE (:search = '' OR p.first_name LIKE CONCAT(:search, '%')
                                OR p.last_name LIKE CONCAT(:search, '%'))
              AND (:filterByDoctor = 0 OR EXISTS (SELECT 1
                                                  FROM visits v
                                                  WHERE v.patient_id = p.id
                                                    AND v.doctor_id IN (:doctorIds)))
            ORDER BY p.id
            """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM patients p
                    WHERE (:search = '' OR p.first_name LIKE CONCAT(:search, '%')
                                        OR p.last_name LIKE CONCAT(:search, '%'))
                      AND (:filterByDoctor = 0 OR EXISTS (SELECT 1
                                                          FROM visits v
                                                          WHERE v.patient_id = p.id
                                                            AND v.doctor_id IN (:doctorIds)))
                    """,
            nativeQuery = true)
    Page<Patient> findPage(@Param("search") String search,
                           @Param("filterByDoctor") int filterByDoctor,
                           @Param("doctorIds") Collection<Long> doctorIds,
                           Pageable pageable);

    Optional<Patient> findByFirstNameAndLastName(String firstName, String lastName);
}
