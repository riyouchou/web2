package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderContainerStatus {

    INITIALIZE("Initialize", 0),
    IN_SERVICE("In Service", 1),
    ORDER_CLOSED_NORMALLY("Order Normally Closed", 2),
    CREDIT_INSUFFICIENT_CLOSED_ABNORMALLY("Insufficient Credit, Abnormally Closed", 3),
    UNPLEDGED_CONTAINER_UNAVAILABLE("Unpledged Container Unavailable", 4),
    ORDER_ABNORMALLY_CLOSED("Order Abnormally Closed", 5)
    ;

    private final String name;
    private final Integer value;
}
