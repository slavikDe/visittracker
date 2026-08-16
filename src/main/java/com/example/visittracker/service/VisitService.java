package com.example.visittracker.service;

import com.example.visittracker.dto.DoctorDto;
import com.example.visittracker.dto.PatientDto;
import com.example.visittracker.dto.VisitDto;
import com.example.visittracker.dto.VisitResponseDto;
import com.example.visittracker.entity.Doctor;
import com.example.visittracker.entity.Patient;
import com.example.visittracker.entity.TimeRange;
import com.example.visittracker.entity.Visit;
import com.example.visittracker.repository.DoctorRepository;
import com.example.visittracker.repository.PatientRepository;
import com.example.visittracker.repository.VisitRepository;
import com.example.visittracker.validation.DateValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;

@RequiredArgsConstructor
@Service
public class VisitService {

    private final VisitRepository visitRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final DateValidator visitValidator;

    @Transactional
    public VisitResponseDto createVisit(VisitDto visitDto) {
        if (visitDto == null) throw badRequest("Visit can't be null");
        if (visitDto.patientId() == null) throw badRequest("Patient id can't be null");
        if (visitDto.doctorId() == null) throw badRequest("Doctor id can't be null");

        Doctor doc = doctorRepository.findByIdForUpdate(visitDto.doctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Doctor with id: " + visitDto.doctorId() + " not found"));

        TimeRange timeRange = visitValidator.validateDates(visitDto, doc.getTimeZone());

        if (visitRepository.existsOverlapping(doc.getId(), timeRange.start(), timeRange.end())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Doctor " + doc.getFirstName() + " " + doc.getLastName()
                            + " already has a visit overlapping "
                            + DateValidator.format(timeRange.start(), doc.getTimeZone())
                            + " - " + DateValidator.format(timeRange.end(), doc.getTimeZone())
                            + " (" + doc.getTimeZone() + ")");
        }

        Patient pat = patientRepository.findById(visitDto.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Patient with id: " + visitDto.patientId() + " not found"));

        Visit visit = visitRepository.save(new Visit(timeRange.start(), timeRange.end(), pat, doc));

        return new VisitResponseDto(
                visit.getId(),
                DateValidator.format(visit.getStartDateTime(), doc.getTimeZone()),
                DateValidator.format(visit.getEndDateTime(), doc.getTimeZone()),
                pat.getId(),
                doc.getId()
        );
    }

    @Transactional
    public Long createDoctor(DoctorDto doctorDto) {
        ZoneId zoneId = visitValidator.parseTimeZone(doctorDto.timezone());

        return doctorRepository
                .findByFirstNameAndLastNameAndTimeZone(doctorDto.firstName(), doctorDto.lastName(), zoneId)
                .map(Doctor::getId)
                .orElseGet(() -> doctorRepository
                        .save(new Doctor(doctorDto.firstName(), doctorDto.lastName(), zoneId))
                        .getId());
    }

    @Transactional
    public Long createPatient(PatientDto patientDto) {
        return patientRepository
                .findByFirstNameAndLastName(patientDto.firstName(), patientDto.lastName())
                .map(Patient::getId)
                .orElseGet(() -> patientRepository
                        .save(new Patient(patientDto.firstName(), patientDto.lastName()))
                        .getId());
    }

    private static ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }
}
