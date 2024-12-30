package com.yx.pass.remote.pms.model.resp.account;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountModelResp {

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
