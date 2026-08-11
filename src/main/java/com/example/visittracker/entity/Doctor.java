package com.example.visittracker.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.ZoneId;
import java.util.TimeZone;

@Getter
@Setter
public class Doctor extends CommonPerson{
    private static int counter = 0;

    private Integer id;
    private ZoneId timeZone;

    public Doctor(String firstName, String lastName, ZoneId timeZone) {
        super(firstName, lastName);
        this.timeZone = timeZone;
        id = ++counter;
    }


}
