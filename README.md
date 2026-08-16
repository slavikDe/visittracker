# Visit Tracker

Tracking system for patient visits to doctors. Java 21, Spring Boot 4, MySQL 8.

## Running

MySQL runs in Docker; the compose credentials match the defaults in `application.yaml`, so no
environment variables are needed.

```bash
docker compose up -d          # MySQL 8.4 on localhost:3306
./mvnw spring-boot:run        # Flyway creates the schema on startup
```

Override the connection with `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` if needed.

### Test data

`dump.sql.gz` contains a generated dataset: **50 doctors, 20 000 patients, 100 000 visits**, spread
over seven timezones and the last twelve months. Roughly 7 000 patients have seen more than one
doctor, 23 000 (patient, doctor) pairs have repeat visits, and ~5 800 patients have no visits at
all — so every branch of the read endpoint has data behind it.

```bash
zcat dump.sql.gz | docker exec -i visittracker-mysql mysql -uroot -proot
```

Visits sit on a per-doctor half-hour slot grid, so the dataset never violates the no-overlap rule
the create endpoint enforces.

### Tests

```bash
./mvnw test
```

Tests start their own MySQL 8.4 via Testcontainers (Docker must be running). The read path depends
on MySQL specifics — `ROW_NUMBER()` windows, descending indexes, `LIKE CONCAT(?, '%')` — so an
in-memory database would not prove much.

## API

### `POST /visit`

```json
{ "start": "2026-09-01T10:00:00", "end": "2026-09-01T10:30:00", "patientId": 1, "doctorId": 7 }
```

`start` and `end` carry **wall-clock time with no offset, in the doctor's timezone**. The doctor is
loaded first, and their zone converts the request into an absolute instant; sending an explicit
offset (`...T10:00:00Z`) is rejected. Visits are stored in UTC and rendered back in the doctor's
zone on the way out.

Intervals are half-open `[start, end)`, so back-to-back visits (10:00–10:30 then 10:30–11:00) are
allowed, while any real overlap for the same doctor is refused with `409`.

### `GET /patients`

| Param       | Default | Meaning                                                        |
|-------------|---------|----------------------------------------------------------------|
| `page`      | `0`     | zero-based page index                                          |
| `size`      | `20`    | patients per page, max 100                                     |
| `search`    | —       | name **prefix**, matched against first or last name            |
| `doctorIds` | —       | comma-separated ids, e.g. `?doctorIds=3,7`                     |

`doctorIds` filters both sides: only patients who visited one of those doctors are returned, and
each patient's `lastVisits` is narrowed to those doctors. `count` is the total matching the query,
not the page size. `lastVisits` holds the patient's most recent visit to *each* doctor they have
seen; `totalPatients` counts distinct patients who ever visited that doctor, across all visits.

```json
{
  "data": [
    { "firstName": "Serhii", "lastName": "Petrenko",
      "lastVisits": [
        { "start": "2025-12-13T13:00:00", "end": "2025-12-13T13:30:00",
          "doctor": { "firstName": "Vitalii", "lastName": "Polishchuk", "totalPatients": 488 } }
      ] }
  ],
  "count": 20000
}
```

### Errors

There are no custom exception types or exception handler. Services throw Spring's
`ResponseStatusException`, and `spring.mvc.problemdetails.enabled` renders everything — application
and framework failures alike — as RFC 9457 problem details:

| Status | When                                                          |
|--------|---------------------------------------------------------------|
| `400`  | bad date format, blank field, `end` ≤ `start`, bad paging, unparseable query param |
| `404`  | unknown doctor or patient, unknown route                       |
| `405`  | wrong HTTP method                                              |
| `409`  | the doctor's time is already taken                             |
| `415`  | unsupported content type                                       |

The trade-off is that the service layer imports `HttpStatus`, so it knows about HTTP. The
alternative — custom exception types plus a `@RestControllerAdvice` — keeps that out of the
services but needs care: a catch-all `@ExceptionHandler(Exception.class)` in an advice runs *ahead*
of Spring's default resolver and will turn 400/404/405/415 into 500 unless the advice also extends
`ResponseEntityExceptionHandler`.

Exceptions that are neither `ResponseStatusException` nor a standard MVC one fall through to Boot's
default error handling, which returns 500 without leaking the message (`server.error.include-message`
defaults to `never`).

## Query strategy

A page costs a **fixed four queries regardless of page size** — the naive shape (loop the page,
query each patient's visits, then each doctor's total) would be `1 + N + M` round trips.

1. `COUNT(*)` of matching patients → `count`
2. the page of patients itself
3. every patient's last visit per doctor, in **one** `ROW_NUMBER()` window query
4. `COUNT(DISTINCT patient_id)` for the doctors that appeared

Supporting indexes, all in `V1__init.sql`:

| Index                                                | Serves                                                  |
|------------------------------------------------------|---------------------------------------------------------|
| `idx_visits_doctor_start (doctor_id, start, end)`     | overlap check on create — index-only, stops at first hit |
| `idx_visits_patient_doctor_start (patient_id, doctor_id, start DESC)` | confines the window to one page of patients |
| `idx_visits_doctor_patient (doctor_id, patient_id)`   | `totalPatients` via loose index scan                     |
| `idx_patients_first_name` / `idx_patients_last_name`  | prefix search, covering the `ORDER BY id` paging         |

Search is a **prefix** match (`LIKE 'x%'`) precisely so these indexes can serve it — a leading
wildcard (`'%x%'`) cannot use a B-tree and would scan the whole table on every request.

Measured on the 100k-visit dataset in `dump.sql.gz`: 22–54 ms per page warm, across plain, `search`,
`doctorIds` and deep-offset (`page=500`) queries.

## Concurrency

The overlap check and the insert are a check-then-act race: two simultaneous requests could both
find the slot free. `createVisit` therefore loads the doctor with `SELECT ... FOR UPDATE`
(`findByIdForUpdate`), which serialises bookings per doctor for the duration of the transaction.

## Known gaps

- Deep pagination uses `LIMIT/OFFSET`; at very high offsets MySQL still walks the skipped rows.
  Keyset pagination (`WHERE id > ?`) would be the fix if deep paging matters.
- `GET /statistic` is a debug helper, not part of the spec. It is capped at 100 rows per collection
  because the original unbounded version would pull the entire visits table into memory.
