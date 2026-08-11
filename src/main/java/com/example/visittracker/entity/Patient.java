package com.example.visittracker.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Patient  extends CommonPerson {
    private static int counter = 0;

    private Integer id;

    public Patient(String firstName, String lastName) {
        super(firstName, lastName);
        id = ++counter;
    }

}
