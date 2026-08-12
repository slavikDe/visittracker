package com.example.visittracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZoneId;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "doctors")
public class Doctor extends CommonPerson {

    @Convert(converter = ZoneIdConverter.class)
    @Column(name = "timezone", nullable = false, length = 64)
    private ZoneId timeZone;

    public Doctor(String firstName, String lastName, ZoneId timeZone) {
        super(firstName, lastName);
        this.timeZone = timeZone;
    }
}
