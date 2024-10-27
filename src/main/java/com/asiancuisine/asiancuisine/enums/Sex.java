package com.asiancuisine.asiancuisine.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Sex {
    MALE(1, "MALE"),
    FEMALE(0, "FEMALE");

    private final int code;
    private final String info;
}


