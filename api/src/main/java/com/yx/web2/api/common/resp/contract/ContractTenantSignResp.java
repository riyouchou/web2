package com.yx.web2.api.common.resp.contract;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ContractTenantSignResp {
    private Long count;
    private String startedTime;
}
