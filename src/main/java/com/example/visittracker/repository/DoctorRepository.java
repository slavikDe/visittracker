package com.example.visittracker.repository;

import com.example.visittracker.entity.Doctor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZoneId;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Locks the doctor row for the rest of the transaction, so two concurrent bookings for the same
     * doctor are serialised. Without it the overlap check and the insert are a check-then-act race
     * and both requests can win.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Doctor d WHERE d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);

    /**
     * Backs the "return the existing doctor instead of inserting a duplicate" rule. The old
     * {@code HashSet.contains} check could never match, because the entities had no equals/hashCode.
     */
    Optional<Doctor> findByFirstNameAndLastNameAndTimeZone(String firstName, String lastName, ZoneId timeZone);
}
