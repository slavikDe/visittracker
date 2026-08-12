CREATE TABLE doctors
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    timezone   VARCHAR(64)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE patients
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    -- Prefix search (LIKE 'x%') on either name. The trailing id keeps the index covering for
    -- the ORDER BY id paging, so a search page never touches the table itself.
    KEY idx_patients_first_name (first_name, id),
    KEY idx_patients_last_name (last_name, id)
) ENGINE = InnoDB;

CREATE TABLE visits
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    -- Stored in UTC; the doctor's timezone is applied only when parsing input and rendering output.
    start_date_time DATETIME(6) NOT NULL,
    end_date_time   DATETIME(6) NOT NULL,
    patient_id      BIGINT      NOT NULL,
    doctor_id       BIGINT      NOT NULL,
    PRIMARY KEY (id),

    -- Overlap check on create: WHERE doctor_id = ? AND start < ? AND ? < end
    KEY idx_visits_doctor_start (doctor_id, start_date_time, end_date_time),

    -- "last visit per (patient, doctor)": restricts the ROW_NUMBER() window to one page of
    -- patients. DESC matches the window's ORDER BY so the rows arrive nearly in order.
    KEY idx_visits_patient_doctor_start (patient_id, doctor_id, start_date_time DESC),

    -- totalPatients: COUNT(DISTINCT patient_id) GROUP BY doctor_id, served index-only.
    KEY idx_visits_doctor_patient (doctor_id, patient_id),

    CONSTRAINT fk_visits_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_visits_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id)
) ENGINE = InnoDB;
