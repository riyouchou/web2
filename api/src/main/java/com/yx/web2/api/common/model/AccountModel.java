package com.yx.web2.api.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountModel {

//    private Long accountId = 102L;
//    private String accountName = "102Account";
//    private String accountEmail = "102Account@163.com";
//    private Long tenantId = 20246L;
//    // when accountType is bd , accountId equals bdAccountId, accountName equals bdAccountName
//    private Long bdAccountId = 100L;
//    private String bdAccountName = "100BDAccount";
//    private Integer accountType = AccountType.Admin.getValue();
//    private String tenantType = "GP";// AccountType.Admin.getTenantType();

    private Long accountId;
    private String accountName;
    private String accountEmail;
    private Long tenantId;
    private String tenantName;
    // when accountType is bd , accountId equals bdAccountId, accountName equals bdAccountName
    private Long bdAccountId;
    private String bdAccountName;
    private Integer accountType;
    private String tenantType;// AccountType.Admin.getTenantType();
    private Long ownerAccountId;
    private String ownerAccountName;
    private String ownerAccountEmail;
}
