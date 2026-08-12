package com.example.visittracker.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "patients")
public class Patient extends CommonPerson {

    public Patient(String firstName, String lastName) {
        super(firstName, lastName);
    }
}
