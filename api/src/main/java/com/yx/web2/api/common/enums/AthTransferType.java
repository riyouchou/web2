package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AthTransferType {
    In(1),
    Rollback(2),
    Out(3);

    private final Integer value;
}
