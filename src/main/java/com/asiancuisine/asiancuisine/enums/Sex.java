package com.asiancuisine.asiancuisine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Sex {
    FEMALE(0, "FEMALE"),
    MALE(1, "MALE");

    private final int code;
    private final String info;
}


