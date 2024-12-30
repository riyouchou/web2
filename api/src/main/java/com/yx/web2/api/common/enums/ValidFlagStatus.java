package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ValidFlagStatus {

    /**
     * 有效
     */
    Effective("Effective", 1),
    /**
     * 无效
     */
    InVain("InVain", 2);

    private final String name;
    private final Integer value;
}
