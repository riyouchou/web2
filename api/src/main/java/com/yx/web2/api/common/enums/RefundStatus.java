package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RefundStatus {

    NOT_REFUNDED("Not Refunded", 0),
    REFUNDING("Refunding", 1),
    REFUNDED("Refunded", 2);

    private final String name;
    private final Integer value;
}
