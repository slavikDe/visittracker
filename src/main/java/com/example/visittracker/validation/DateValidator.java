package com.example.visittracker.validation;

import com.example.visittracker.dto.VisitDto;
import com.example.visittracker.entity.TimeRange;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class DateValidator {

    public static final DateTimeFormatter VISIT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static String format(Instant instant, ZoneId zone) {
        return VISIT_TIME.format(LocalDateTime.ofInstant(instant, zone));
    }

    public TimeRange validateDates(VisitDto visitDto, ZoneId doctorZone) {
        LocalDateTime start = parseDateTime(visitDto.start(), "start");
        LocalDateTime end = parseDateTime(visitDto.end(), "end");

        if (!start.isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Visit start must be before its end");
        }

        return new TimeRange(
                start.atZone(doctorZone).toInstant(),
                end.atZone(doctorZone).toInstant()
        );
    }

    public ZoneId parseTimeZone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Doctor timezone can't be empty");
        }

        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Doctor timezone must be a valid zone id, e.g. Europe/Kyiv, but was: " + timezone);
        }
    }

    private LocalDateTime parseDateTime(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Visit " + field + " can't be empty");
        }

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Visit " + field + " must be a date time without offset in the doctor's timezone,"
                            + " e.g. 2026-08-06T10:00:00, but was: " + value);
        }
    }
}
