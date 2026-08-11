package com.example.visittracker.service;

import com.example.visittracker.entity.Doctor;
import com.example.visittracker.repository.MockDoctorRepository;
import com.example.visittracker.repository.MockPatientRepository;
import com.example.visittracker.repository.MockVisitRepository;
import com.example.visittracker.validation.VisitValidator;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class VisitServiceTest {

    private VisitService visitService;

    @Mock
    private MockVisitRepository visitRepository;

    @Mock
    private MockDoctorRepository doctorRepository;

    @Mock
    private MockPatientRepository patientRepository;
//

    void setUp() {
        visitRepository = new MockVisitRepository();
        doctorRepository = new MockDoctorRepository();
        patientRepository = new MockPatientRepository();

        visitService = new VisitService(
                visitRepository,
                doctorRepository,
                patientRepository,
                new VisitValidator()
        );
    }

    @Test
    public void createDoctor_shouldCreateValidDoctor() {
//        Doctor doctor = new Doctor("name", "surname", TimeZone.getTimeZone());
//
//        when(visitService.createDoctor(doctor)).thenReturn(1);
    }


}