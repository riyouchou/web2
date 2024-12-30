package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountType {
    Admin(30),
    GAMING_BD(31),
    FINANCE(32),
    GAMING_TENANT_OWNER(33),
    GAMING_TENANT_USER(34),
    AI_BD(37),
    AI_TENANT_OWNER(38),
    AI_TENANT_USER(39);
    private final Integer value;
}
