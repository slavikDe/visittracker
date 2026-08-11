package com.example.visittracker.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public abstract class CommonPerson {
    protected String firstName;
    protected String lastName;
}
