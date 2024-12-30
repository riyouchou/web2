package com.yx.web2.api.service;

import com.yx.web2.api.common.enums.ServiceDurationPeriod;
import lombok.experimental.UtilityClass;

import java.util.Objects;

@UtilityClass
public class ServiceDurationCalculate {
    public int calculate(Integer serviceDuration, Integer serviceDurationPeriod) {
        int totalHours = 0;
        switch (Objects.requireNonNull(ServiceDurationPeriod.valueOf(serviceDurationPeriod))) {
            case Day:
                totalHours = 24 * serviceDuration;
                break;
            case Week:
                totalHours = 24 * 7 * serviceDuration;
                break;
            case Month:
                totalHours = 730 * serviceDuration;
                break;
            default: // year
                totalHours = 730 * 12 * serviceDuration;
                break;
        }
        return totalHours;
    }
}
