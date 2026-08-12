package com.example.visittracker.service;

import com.example.visittracker.dto.DoctorDto;
import com.example.visittracker.dto.PatientDto;
import com.example.visittracker.dto.StatisticDto;
import com.example.visittracker.dto.VisitDto;
import com.example.visittracker.entity.Doctor;
import com.example.visittracker.entity.Patient;
import com.example.visittracker.entity.TimeRange;
import com.example.visittracker.entity.Visit;
import com.example.visittracker.exception.NotFoundException;
import com.example.visittracker.exception.SlotTakenException;
import com.example.visittracker.repository.MockDoctorRepository;
import com.example.visittracker.repository.MockPatientRepository;
import com.example.visittracker.repository.MockVisitRepository;
import com.example.visittracker.validation.DateValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RequiredArgsConstructor
@Service
public class VisitService {
    private final MockVisitRepository visitRepository;
    private final MockDoctorRepository doctorRepository;
    private final MockPatientRepository patientRepository;
    private final DateValidator visitValidator;

    public List<Visit> getVisitsByDoctorId(Integer doctorId) {
        doctorRepository.getDoctorById(doctorId);

        List<Visit> visits = visitRepository.getVisitsByDoctorId(doctorId);
        if (visits.isEmpty()) {
            throw new NotFoundException("Not found visits for doctor with id:" + doctorId);
        }

        return visits;
    }

    public Visit createVisit(VisitDto visitDto) {
        if (visitDto == null) throw new IllegalArgumentException("Visit can't be null");
        if (visitDto.patientId() == null) throw new IllegalArgumentException("Patient id can't be null");
        if (visitDto.doctorId() == null) throw new IllegalArgumentException("Doctor id can't be null");

        Doctor doc = doctorRepository.getDoctorById(visitDto.doctorId());
        TimeRange timeRange = visitValidator.validateDates(visitDto, doc.getTimeZone());
        if (visitRepository.existsOverlappingVisit(doc.getId(), timeRange)) {
            throw new SlotTakenException(
                    "Doctor " + doc.getFirstName() + " " + doc.getLastName()
                            + " already has a visit between " + inDoctorZone(timeRange.start(), doc)
                            + " and " + inDoctorZone(timeRange.end(), doc)
                            + " (" + doc.getTimeZone() + ")");
        }

        Patient pat = patientRepository.getPatientById(visitDto.patientId());

        return visitRepository.saveVisit(new Visit(timeRange.start(), timeRange.end(), pat, doc));
    }

    /** Renders an instant back as the doctor's local time, so errors echo what the caller sent. */
    private String inDoctorZone(Instant instant, Doctor doctor) {
        return LocalDateTime.ofInstant(instant, doctor.getTimeZone()).toString();
    }

    public Integer createDoctor(DoctorDto doctorDto) {
        ZoneId zoneId = visitValidator.parseTimeZone(doctorDto.timezone());

        Doctor doctor = new Doctor(
                doctorDto.firstName(),
                doctorDto.lastName(),
                zoneId
        );

        if (doctorRepository.exists(doctor)) {
            return doctor.getId();
        }

        return doctorRepository.saveDoctor(doctor);
    }

    public Integer createPatient(PatientDto patientDto) {
        Patient patient = new Patient(
                patientDto.firstName(),
                patientDto.lastName()
        );

        if (patientRepository.exists(patient)) {
            return patient.getId();
        }

        return patientRepository.savePatient(patient);
    }

    public StatisticDto getStatistic() {
        return new StatisticDto(
                visitRepository.getAllVisits().stream().map(v -> new VisitDto(
                        v.getStartDateTime().toString(),
                        v.getEndDateTime().toString(),
                        v.getPatient().getId(),
                        v.getDoctor().getId()
                )).toList(),
                doctorRepository.getAllDoctors().stream().map(d -> new DoctorDto(
                        d.getFirstName(),
                        d.getLastName(),
                        d.getTimeZone().toString()
                )).toList(),
                patientRepository.getAllPatients().stream().map(p -> new PatientDto(
                        p.getFirstName(),
                        p.getLastName()
                )).toList()
        );
    }
}
