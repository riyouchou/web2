package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderContainerTopicStatus {

    CREATE("CREATE", 1),
    WAITING_PAYMENT("Waiting Payment", 2),
    CLOSED("Closed", 3),
    IN_SERVICE("In Service", 4),
    ABNORMAL_CLOSED("Abnormal Closed", 5),
    CREDIT_INSUFFICIENT_CLOSED_ABNORMALLY("Credit Insufficient Closed Abnormally", 6);

    private final String name;
    private final Integer value;
}
