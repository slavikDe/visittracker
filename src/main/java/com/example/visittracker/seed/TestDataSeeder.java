package com.example.visittracker.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Generates the test dataset, under the {@code seed} profile only.
 * <p>
 * Two properties of the generated data matter for exercising the API:
 * <ul>
 *   <li>every doctor is seen by many patients, and every patient sees several doctors — so
 *       {@code totalPatients} and multi-doctor {@code lastVisits} are non-trivial;</li>
 *   <li>patients visit the <em>same</em> doctor more than once, so "last visit per doctor" actually
 *       has something to pick from.</li>
 * </ul>
 * Visits are laid out on a per-doctor slot grid, so the seeded rows never violate the no-overlap
 * rule the create endpoint enforces.
 */
@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class TestDataSeeder implements ApplicationRunner {

    private static final List<String> TIMEZONES = List.of(
            "Europe/Kyiv", "Europe/London", "Europe/Berlin", "America/New_York",
            "America/Los_Angeles", "Asia/Tokyo", "Australia/Sydney");

    private static final List<String> FIRST_NAMES = List.of(
            "Ivan", "Olha", "Petro", "Anna", "Mykola", "Kateryna", "Andrii", "Sofiia",
            "Dmytro", "Iryna", "Serhii", "Tetiana", "Oleksandr", "Nataliia", "Yurii",
            "Mariia", "Vitalii", "Oksana", "Roman", "Liudmyla");

    private static final List<String> LAST_NAMES = List.of(
            "Petrenko", "Shevchenko", "Kovalenko", "Bondarenko", "Tkachenko", "Kravchenko",
            "Oliinyk", "Shevchuk", "Polishchuk", "Boiko", "Moroz", "Lysenko", "Rudenko",
            "Melnyk", "Savchenko", "Marchenko", "Pavlenko", "Zakharchenko", "Ivanenko", "Danylenko");

    /** Working day: 16 half-hour slots from 09:00 to 17:00 in the doctor's own local time. */
    private static final int SLOTS_PER_DAY = 16;
    private static final int FIRST_SLOT_HOUR = 9;
    private static final int SLOT_MINUTES = 30;
    private static final int BATCH_SIZE = 1000;

    /** How many distinct patients each doctor draws from. Small enough to guarantee repeat visits. */
    private static final int PANEL_SIZE = 500;

    private final JdbcTemplate jdbc;
    private final ApplicationContext applicationContext;

    @Value("${seed.doctors:50}")
    private int doctorCount;

    @Value("${seed.patients:20000}")
    private int patientCount;

    @Value("${seed.visits:100000}")
    private int visitCount;

    @Value("${seed.seed:42}")
    private long randomSeed;

    @Value("${seed.exit:true}")
    private boolean exitWhenDone;

    @Override
    public void run(ApplicationArguments args) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM visits", Long.class) > 0) {
            log.warn("Visits table is not empty, skipping seeding. Truncate first to reseed.");
            finish();
            return;
        }

        Random random = new Random(randomSeed);
        log.info("Seeding {} doctors, {} patients, {} visits...", doctorCount, patientCount, visitCount);

        List<ZoneId> doctorZones = insertDoctors(random);
        insertPatients(random);
        insertVisits(random, doctorZones);

        log.info("Seeding complete.");
        finish();
    }

    private void finish() {
        if (exitWhenDone) {
            SpringApplication.exit(applicationContext, () -> 0);
        }
    }

    private List<ZoneId> insertDoctors(Random random) {
        List<Object[]> rows = new ArrayList<>(doctorCount);
        List<ZoneId> zones = new ArrayList<>(doctorCount);

        for (int i = 0; i < doctorCount; i++) {
            String zone = TIMEZONES.get(i % TIMEZONES.size());
            zones.add(ZoneId.of(zone));
            rows.add(new Object[]{pick(FIRST_NAMES, random), pick(LAST_NAMES, random), zone});
        }

        jdbc.batchUpdate("INSERT INTO doctors (first_name, last_name, timezone) VALUES (?, ?, ?)", rows);
        return zones;
    }

    private void insertPatients(Random random) {
        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < patientCount; i++) {
            batch.add(new Object[]{pick(FIRST_NAMES, random), pick(LAST_NAMES, random)});

            if (batch.size() == BATCH_SIZE) {
                jdbc.batchUpdate("INSERT INTO patients (first_name, last_name) VALUES (?, ?)", batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO patients (first_name, last_name) VALUES (?, ?)", batch);
        }
    }

    private void insertVisits(Random random, List<ZoneId> doctorZones) {
        long firstDoctorId = jdbc.queryForObject("SELECT MIN(id) FROM doctors", Long.class);
        long firstPatientId = jdbc.queryForObject("SELECT MIN(id) FROM patients", Long.class);

        LocalDate startDate = LocalDate.now().minusYears(1);
        int visitsPerDoctor = Math.max(1, visitCount / doctorCount);

        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        int written = 0;

        for (int d = 0; d < doctorCount && written < visitCount; d++) {
            long doctorId = firstDoctorId + d;
            ZoneId doctorZone = doctorZones.get(d);
            List<Long> panel = buildPanel(random, firstPatientId);

            for (int slot = 0; slot < visitsPerDoctor && written < visitCount; slot++) {
                // Sequential slots on the grid never overlap each other.
                LocalDateTime localStart = startDate
                        .plusDays(slot / SLOTS_PER_DAY)
                        .atTime(FIRST_SLOT_HOUR, 0)
                        .plusMinutes((long) (slot % SLOTS_PER_DAY) * SLOT_MINUTES);

                Instant start = localStart.atZone(doctorZone).toInstant();
                Instant end = start.plusSeconds(SLOT_MINUTES * 60L);

                batch.add(new Object[]{
                        utcWallClock(start), utcWallClock(end),
                        panel.get(random.nextInt(panel.size())), doctorId});
                written++;

                if (batch.size() == BATCH_SIZE) {
                    flushVisits(batch);
                }
            }
        }
        if (!batch.isEmpty()) {
            flushVisits(batch);
        }

        log.info("Inserted {} visits.", written);
    }

    /** A doctor's recurring patients: drawing visits from this pool guarantees repeat visits. */
    private List<Long> buildPanel(Random random, long firstPatientId) {
        int size = Math.min(PANEL_SIZE, patientCount);
        Set<Long> panel = new LinkedHashSet<>(size * 2);

        while (panel.size() < size) {
            panel.add(firstPatientId + random.nextInt(patientCount));
        }
        return new ArrayList<>(panel);
    }

    private void flushVisits(List<Object[]> batch) {
        jdbc.batchUpdate("""
                INSERT INTO visits (start_date_time, end_date_time, patient_id, doctor_id)
                VALUES (?, ?, ?, ?)
                """, batch);
        batch.clear();
    }

    /**
     * Binds the instant as a UTC wall-clock value, matching how Hibernate writes the column.
     * <p>
     * It must be a {@link LocalDateTime}, which the driver writes verbatim. Converting to
     * {@code Timestamp} first would re-interpret the value in the JVM's default zone and shift
     * every seeded visit by the machine's offset.
     */
    private static LocalDateTime utcWallClock(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static String pick(List<String> values, Random random) {
        return values.get(random.nextInt(values.size()));
    }
}
