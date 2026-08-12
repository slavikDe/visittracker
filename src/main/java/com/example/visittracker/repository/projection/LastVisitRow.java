package com.example.visittracker.repository.projection;

import java.time.Instant;

/**
 * One "last visit of patient X to doctor Y" row, with the doctor's columns already joined in so
 * the caller never has to fetch doctors separately.
 * <p>
 * The timestamps are projected as {@link Instant}, not {@code LocalDateTime}, on purpose. The
 * driver reads the UTC {@code DATETIME} column into a {@code Timestamp} whose epoch is correct,
 * but converting that to {@code LocalDateTime} re-interprets it in the JVM's default zone — which
 * silently shifts every time by the server's offset. Going through the epoch keeps the JVM's own
 * timezone out of the result.
 */
public interface LastVisitRow {

    Long getPatientId();

    Long getDoctorId();

    Instant getStartDateTime();

    Instant getEndDateTime();

    String getDoctorFirstName();

    String getDoctorLastName();

    String getDoctorTimezone();
}
