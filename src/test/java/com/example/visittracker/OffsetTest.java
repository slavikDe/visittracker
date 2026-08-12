package com.example.visittracker;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins down the offset arithmetic the whole timezone contract rests on, without needing a database.
 */
public class OffsetTest {

    @Test
    public void localTimeResolvesThroughDoctorZone() {
        LocalDateTime local = LocalDateTime.parse("2026-08-06T10:00:00");

        // August: Kyiv is UTC+3, London is UTC+1.
        assertThat(local.atZone(ZoneId.of("Europe/Kyiv")).toInstant())
                .isEqualTo(LocalDateTime.parse("2026-08-06T07:00:00").toInstant(ZoneOffset.UTC));
        assertThat(local.atZone(ZoneId.of("Europe/London")).toInstant())
                .isEqualTo(LocalDateTime.parse("2026-08-06T09:00:00").toInstant(ZoneOffset.UTC));
    }

    @Test
    public void sameZoneShiftsAcrossDaylightSaving() {
        // January: Kyiv is UTC+2, so the same wall clock maps to a different instant than in August.
        LocalDateTime winter = LocalDateTime.parse("2026-01-06T10:00:00");

        assertThat(winter.atZone(ZoneId.of("Europe/Kyiv")).toInstant())
                .isEqualTo(LocalDateTime.parse("2026-01-06T08:00:00").toInstant(ZoneOffset.UTC));
    }
}
