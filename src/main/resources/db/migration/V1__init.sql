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

    KEY idx_patients_first_name (first_name, id),
    KEY idx_patients_last_name (last_name, id)
) ENGINE = InnoDB;

CREATE TABLE visits
(
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    start_date_time DATETIME(6) NOT NULL,
    end_date_time   DATETIME(6) NOT NULL,
    patient_id      BIGINT      NOT NULL,
    doctor_id       BIGINT      NOT NULL,
    PRIMARY KEY (id),

    KEY idx_visits_doctor_start (doctor_id, start_date_time, end_date_time),
    KEY idx_visits_patient_doctor_start (patient_id, doctor_id, start_date_time DESC),
    KEY idx_visits_doctor_patient (doctor_id, patient_id),

    CONSTRAINT fk_visits_patient FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT fk_visits_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id)
) ENGINE = InnoDB;
