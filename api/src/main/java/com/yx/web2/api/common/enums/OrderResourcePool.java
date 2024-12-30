package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderResourcePool {
    ARS(1),
    BM(2);

    private final Integer value;
}
