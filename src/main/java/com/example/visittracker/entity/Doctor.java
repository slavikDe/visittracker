package com.example.visittracker.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Doctor extends CommonPerson{
    private Integer id;
    private String timeZone;

}
