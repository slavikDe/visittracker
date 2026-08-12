package com.example.visittracker.service;

import com.example.visittracker.AbstractMySqlTest;
import com.example.visittracker.dto.LastVisitDto;
import com.example.visittracker.dto.PatientListResponse;
import com.example.visittracker.dto.PatientVisitsDto;
import com.example.visittracker.entity.Doctor;
import com.example.visittracker.entity.Patient;
import com.example.visittracker.entity.Visit;
import com.example.visittracker.repository.DoctorRepository;
import com.example.visittracker.repository.PatientRepository;
import com.example.visittracker.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static com.example.visittracker.ProblemAssertions.statusIs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientQueryServiceTest extends AbstractMySqlTest {

    @Autowired
    private PatientQueryService patientQueryService;
    @Autowired
    private VisitRepository visitRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private DoctorRepository doctorRepository;

    private Doctor kyivDoctor;
    private Doctor londonDoctor;
    private Patient ivan;
    private Patient olha;
    private Patient petro;

    /**
     * Ivan has seen both doctors (the Kyiv one twice); Olha only the Kyiv doctor; Petro nobody.
     * So totalPatients is 2 for the Kyiv doctor and 1 for the London one.
     */
    @BeforeEach
    void setUp() {
        visitRepository.deleteAllInBatch();
        patientRepository.deleteAllInBatch();
        doctorRepository.deleteAllInBatch();

        kyivDoctor = doctorRepository.save(new Doctor("Anna", "Kovalenko", ZoneId.of("Europe/Kyiv")));
        londonDoctor = doctorRepository.save(new Doctor("John", "Smith", ZoneId.of("Europe/London")));

        ivan = patientRepository.save(new Patient("Ivan", "Petrenko"));
        olha = patientRepository.save(new Patient("Olha", "Shevchenko"));
        petro = patientRepository.save(new Patient("Petro", "Ivanenko"));

        saveVisit(ivan, kyivDoctor, "2026-08-01T07:00:00Z");
        saveVisit(ivan, kyivDoctor, "2026-08-05T07:00:00Z");   // the most recent one
        saveVisit(ivan, londonDoctor, "2026-08-03T09:00:00Z");
        saveVisit(olha, kyivDoctor, "2026-08-02T07:00:00Z");
    }

    private void saveVisit(Patient patient, Doctor doctor, String startUtc) {
        Instant start = Instant.parse(startUtc);
        visitRepository.save(new Visit(start, start.plusSeconds(1800), patient, doctor));
    }

    @Test
    void getPatients_returnsEveryPatientWithCountByDefault() {
        PatientListResponse response = patientQueryService.getPatients(null, null, null, null);

        assertThat(response.count()).isEqualTo(3);
        assertThat(response.data()).extracting(PatientVisitsDto::firstName)
                .containsExactly("Ivan", "Olha", "Petro");
    }

    @Test
    void getPatients_returnsOnlyTheLastVisitPerDoctor() {
        PatientVisitsDto result = onlyPatient(patientQueryService.getPatients(null, null, "Petrenko", null));

        // Two doctors seen, three visits made -> two entries, the Kyiv one being the later visit.
        assertThat(result.lastVisits()).hasSize(2);

        LastVisitDto kyivVisit = visitTo(result, "Anna");
        assertThat(kyivVisit.start()).isEqualTo("2026-08-05T10:00:00");
    }

    @Test
    void getPatients_rendersTimesInEachDoctorsOwnTimezone() {
        PatientVisitsDto result = onlyPatient(patientQueryService.getPatients(null, null, "Petrenko", null));

        // Same stored instants, two different zones: Kyiv is UTC+3 in August, London UTC+1.
        assertThat(visitTo(result, "Anna").start()).isEqualTo("2026-08-05T10:00:00");
        assertThat(visitTo(result, "John").start()).isEqualTo("2026-08-03T10:00:00");
    }

    @Test
    void getPatients_reportsTotalPatientsPerDoctorAcrossAllVisits() {
        PatientVisitsDto result = onlyPatient(patientQueryService.getPatients(null, null, "Petrenko", null));

        assertThat(visitTo(result, "Anna").doctor().totalPatients()).isEqualTo(2);
        assertThat(visitTo(result, "John").doctor().totalPatients()).isEqualTo(1);
    }

    @Test
    void getPatients_searchMatchesPrefixOfFirstOrLastName() {
        // "Ivan" is Ivan Petrenko's first name and the prefix of Petro Ivanenko's last name.
        PatientListResponse response = patientQueryService.getPatients(null, null, "Ivan", null);

        assertThat(response.count()).isEqualTo(2);
        assertThat(response.data()).extracting(PatientVisitsDto::firstName)
                .containsExactlyInAnyOrder("Ivan", "Petro");
    }

    @Test
    void getPatients_searchDoesNotMatchMidWord() {
        assertThat(patientQueryService.getPatients(null, null, "trenko", null).count()).isZero();
    }

    @Test
    void getPatients_doctorIdsFilterBothPatientsAndVisits() {
        PatientListResponse response =
                patientQueryService.getPatients(null, null, null, List.of(londonDoctor.getId()));

        // Only Ivan has seen the London doctor, and only that visit is listed.
        assertThat(response.count()).isEqualTo(1);
        PatientVisitsDto result = onlyPatient(response);
        assertThat(result.firstName()).isEqualTo("Ivan");
        assertThat(result.lastVisits()).hasSize(1);
        assertThat(result.lastVisits().getFirst().doctor().firstName()).isEqualTo("John");
    }

    @Test
    void getPatients_patientWithoutVisitsHasEmptyList() {
        // "Petro" would also match Ivan *Petrenko*, so search the surname instead.
        PatientVisitsDto result = onlyPatient(patientQueryService.getPatients(null, null, "Ivanenko", null));

        assertThat(result.lastVisits()).isEmpty();
    }

    @Test
    void getPatients_paginatesWhileCountStaysTotal() {
        PatientListResponse firstPage = patientQueryService.getPatients(0, 2, null, null);
        PatientListResponse secondPage = patientQueryService.getPatients(1, 2, null, null);

        assertThat(firstPage.data()).hasSize(2);
        assertThat(secondPage.data()).hasSize(1);
        assertThat(firstPage.count()).isEqualTo(3);
        assertThat(secondPage.count()).isEqualTo(3);
    }

    @Test
    void getPatients_returnsEmptyPagePastTheEnd() {
        PatientListResponse response = patientQueryService.getPatients(10, 20, null, null);

        assertThat(response.data()).isEmpty();
        assertThat(response.count()).isEqualTo(3);
    }

    @Test
    void getPatients_rejectsInvalidPaging() {
        assertThatThrownBy(() -> patientQueryService.getPatients(-1, 20, null, null))
                .satisfies(statusIs(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Page number");

        assertThatThrownBy(() -> patientQueryService.getPatients(0, 0, null, null))
                .satisfies(statusIs(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Page size");

        assertThatThrownBy(() -> patientQueryService.getPatients(0, 5000, null, null))
                .satisfies(statusIs(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Page size");
    }

    private static PatientVisitsDto onlyPatient(PatientListResponse response) {
        assertThat(response.data()).hasSize(1);
        return response.data().getFirst();
    }

    private static LastVisitDto visitTo(PatientVisitsDto patient, String doctorFirstName) {
        return patient.lastVisits().stream()
                .filter(v -> v.doctor().firstName().equals(doctorFirstName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No visit to " + doctorFirstName));
    }
}
