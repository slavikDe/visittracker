package com.example.visittracker.service;

import com.example.visittracker.AbstractMySqlTest;
import com.example.visittracker.dto.DoctorDto;
import com.example.visittracker.dto.PatientDto;
import com.example.visittracker.dto.VisitDto;
import com.example.visittracker.dto.VisitResponseDto;
import com.example.visittracker.entity.Visit;
import com.example.visittracker.repository.DoctorRepository;
import com.example.visittracker.repository.PatientRepository;
import com.example.visittracker.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.Instant;

import static com.example.visittracker.ProblemAssertions.statusIs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VisitServiceTest extends AbstractMySqlTest {

    @Autowired
    private VisitService visitService;
    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DoctorRepository doctorRepository;

    private Long kyivDoctorId;
    private Long patientId;

    @BeforeEach
    void setUp() {
        visitRepository.deleteAllInBatch();
        patientRepository.deleteAllInBatch();
        doctorRepository.deleteAllInBatch();

        kyivDoctorId = visitService.createDoctor(new DoctorDto("Anna", "Kovalenko", "Europe/Kyiv"));
        patientId = visitService.createPatient(new PatientDto("Ivan", "Petrenko"));
    }

    @Test
    void createVisit_interpretsRequestTimeInDoctorsTimezone() {
        VisitResponseDto created = visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T10:30:00", patientId, kyivDoctorId));

        // Europe/Kyiv is UTC+3 in August, so 10:00 local is stored as 07:00Z.
        Visit stored = visitRepository.findById(created.id()).orElseThrow();
        assertThat(stored.getStartDateTime()).isEqualTo(Instant.parse("2026-08-06T07:00:00Z"));
        assertThat(stored.getEndDateTime()).isEqualTo(Instant.parse("2026-08-06T07:30:00Z"));

        // ...but the response echoes the doctor's local time back, as sent.
        assertThat(created.start()).isEqualTo("2026-08-06T10:00:00");
        assertThat(created.end()).isEqualTo("2026-08-06T10:30:00");
    }

    @Test
    void createVisit_rejectsOverlapForSameDoctor() {
        visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", patientId, kyivDoctorId));

        Long otherPatient = visitService.createPatient(new PatientDto("Olha", "Shevchenko"));

        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:30:00", "2026-08-06T11:30:00", otherPatient, kyivDoctorId)))
                .satisfies(statusIs(HttpStatus.CONFLICT))
                .hasMessageContaining("Anna Kovalenko");
    }

    @Test
    void createVisit_rejectsVisitFullyContainingAnExistingOne() {
        visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T10:30:00", patientId, kyivDoctorId));

        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T09:00:00", "2026-08-06T12:00:00", patientId, kyivDoctorId)))
                .satisfies(statusIs(HttpStatus.CONFLICT));
    }

    @Test
    void createVisit_allowsBackToBackVisits() {
        visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T10:30:00", patientId, kyivDoctorId));

        VisitResponseDto next = visitService.createVisit(
                new VisitDto("2026-08-06T10:30:00", "2026-08-06T11:00:00", patientId, kyivDoctorId));

        assertThat(next.id()).isNotNull();
    }

    @Test
    void createVisit_allowsSameLocalTimeForDifferentDoctors() {
        Long londonDoctorId = visitService.createDoctor(new DoctorDto("John", "Smith", "Europe/London"));

        visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", patientId, kyivDoctorId));
        VisitResponseDto londonVisit = visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", patientId, londonDoctorId));

        // Same wall clock, different zones, so two hours apart on the absolute timeline.
        Visit stored = visitRepository.findById(londonVisit.id()).orElseThrow();
        assertThat(stored.getStartDateTime()).isEqualTo(Instant.parse("2026-08-06T09:00:00Z"));
    }

    @Test
    void createVisit_rejectsTimeWithExplicitOffset() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00Z", "2026-08-06T11:00:00", patientId, kyivDoctorId)))
                .satisfies(statusIs(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("without offset");
    }

    @Test
    void createVisit_rejectsEndBeforeStart() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T11:00:00", "2026-08-06T10:00:00", patientId, kyivDoctorId)))
                .satisfies(statusIs(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("start must be before its end");
    }

    @Test
    void createVisit_rejectsZeroLengthVisit() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T10:00:00", patientId, kyivDoctorId)))
                .satisfies(statusIs(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createVisit_rejectsUnknownDoctor() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", patientId, 9999L)))
                .satisfies(statusIs(HttpStatus.NOT_FOUND))
                .hasMessageContaining("Doctor with id: 9999");
    }

    @Test
    void createVisit_rejectsUnknownPatient() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", 9999L, kyivDoctorId)))
                .satisfies(statusIs(HttpStatus.NOT_FOUND))
                .hasMessageContaining("Patient with id: 9999");
    }

    @Test
    void createVisit_rejectsMissingIdsAndNullPayload() {
        assertThatThrownBy(() -> visitService.createVisit(null))
                .satisfies(statusIs(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", null, kyivDoctorId)))
                .satisfies(statusIs(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Patient id");

        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto("2026-08-06T10:00:00", "2026-08-06T11:00:00", patientId, null)))
                .satisfies(statusIs(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Doctor id");
    }

    @Test
    void createVisit_rejectsBlankDates() {
        assertThatThrownBy(() -> visitService.createVisit(
                new VisitDto(null, "2026-08-06T11:00:00", patientId, kyivDoctorId)))
                .satisfies(statusIs(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("start can't be empty");
    }

    @Test
    void createDoctor_returnsExistingIdInsteadOfDuplicating() {
        Long again = visitService.createDoctor(new DoctorDto("Anna", "Kovalenko", "Europe/Kyiv"));

        assertThat(again).isEqualTo(kyivDoctorId);
        assertThat(doctorRepository.count()).isEqualTo(1);
    }
}
