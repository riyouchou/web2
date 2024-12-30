package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ServiceDurationPeriod {

    Day(1),
    Week(2),
    Month(3),
    Year(4);

    public static ServiceDurationPeriod valueOf(int val) {
        switch (val) {
            case 1:
                return Day;
            case 2:
                return Week;
            case 3:
                return Month;
            case 4:
                return Year;
            default:
                return null;
        }
    }

    private final Integer value;
}
