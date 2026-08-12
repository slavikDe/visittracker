package com.example.visittracker.service;

import com.example.visittracker.dto.DoctorDto;
import com.example.visittracker.dto.PatientDto;
import com.example.visittracker.dto.VisitDto;
import com.example.visittracker.entity.Visit;
import com.example.visittracker.exception.NotFoundException;
import com.example.visittracker.exception.SlotTakenException;
import com.example.visittracker.repository.MockDoctorRepository;
import com.example.visittracker.repository.MockPatientRepository;
import com.example.visittracker.repository.MockVisitRepository;
import com.example.visittracker.validation.DateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The repositories are in-memory fakes already, so these run as plain unit tests without a
 * Spring context or Mockito stubbing.
 */
class VisitServiceTest {

    private VisitService visitService;

    private Integer kyivDoctorId;
    private Integer kyivPatientId;

    @BeforeEach
    void setUp() {
        visitService = new VisitService(
                new MockVisitRepository(),
                new MockDoctorRepository(),
                new MockPatientRepository(),
                new DateValidator()
        );

        kyivDoctorId = visitService.createDoctor(new DoctorDto("Anna", "Kovalenko", "Europe/Kyiv"));
        kyivPatientId = visitService.createPatient(new PatientDto("Ivan", "Petrenko"));
    }

    @Test
    void createVisit_interpretsRequestTimeInDoctorsTimezone() {
        Visit visit = visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T10:30:00", kyivPatientId, kyivDoctorId));

        // Europe/Kyiv is UTC+3 in August, so 10:00 local is 07:00Z.
        assertThat(visit.getStartDateTime()).isEqualTo(Instant.parse("2026-08-06T07:00:00Z"));
        assertThat(visit.getEndDateTime()).isEqualTo(Instant.parse("2026-08-06T07:30:00Z"));
    }

    @Test
    void createVisit_rejectsOverlapForSameDoctor() {
        visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", kyivPatientId, kyivDoctorId));

        Integer otherPatient = visitService.createPatient(new PatientDto("Olha", "Shevchenko"));

        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:30:00", "2026-08-06T11:30:00", otherPatient, kyivDoctorId)))
                .isInstanceOf(SlotTakenException.class)
                .hasMessageContaining("Anna Kovalenko");
    }

    @Test
    void createVisit_allowsBackToBackVisits() {
        visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T10:30:00", kyivPatientId, kyivDoctorId));

        Visit next = visitService.createVisit(
                new VisitDto("2026-08-06T10:30:00", "2026-08-06T11:00:00", kyivPatientId, kyivDoctorId));

        assertThat(next.getId()).isNotNull();
    }

    @Test
    void createVisit_allowsSameLocalTimeForDifferentDoctors() {
        Integer londonDoctorId = visitService.createDoctor(
                new DoctorDto("John", "Smith", "Europe/London"));

        visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", kyivPatientId, kyivDoctorId));
        Visit londonVisit = visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", kyivPatientId, londonDoctorId));

        // Same wall clock, different zones, so they are two hours apart on the absolute timeline.
        assertThat(londonVisit.getStartDateTime()).isEqualTo(Instant.parse("2026-08-06T09:00:00Z"));
    }

    @Test
    void createVisit_rejectsTimeWithExplicitOffset() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00Z", "2026-08-06T11:00:00", kyivPatientId, kyivDoctorId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("without offset");
    }

    @Test
    void createVisit_rejectsEndBeforeStart() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T11:00:00", "2026-08-06T10:00:00", kyivPatientId, kyivDoctorId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("start must be before its end");
    }

    @Test
    void createVisit_rejectsZeroLengthVisit() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T10:00:00", kyivPatientId, kyivDoctorId)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createVisit_rejectsUnknownDoctor() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", kyivPatientId, 9999)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Doctor with id: 9999");
    }

    @Test
    void createVisit_rejectsUnknownPatient() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", 9999, kyivDoctorId)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Patient with id: 9999");
    }

    @Test
    void createVisit_rejectsMissingIdsAndNullPayload() {
        assertThatThrownBy(() -> visitService.createVisit(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", null, kyivDoctorId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient id");

        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", kyivPatientId, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Doctor id");
    }

    @Test
    void createVisit_rejectsBlankDates() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto(null, "2026-08-06T11:00:00", kyivPatientId, kyivDoctorId)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("start can't be empty");
    }
}
