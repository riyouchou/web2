package com.yx.web2.api.common.resp.contract;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ContractListResp {
    private String contractId;
    private Long contractTenantId;
    private Integer contractStatus;
    private String accountName;
    private String bdName;
    private String period;
    private String startTime;
    private String orderId;
    private String amount;
    private String description;
    private String createTime;
}
