package com.yx.web2.api.common.req.contract;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractAuditReq {
    private String contractId;
    private boolean pass;
    private String rejectMsg;
    private String contractAuthorizeCallbackUrl;
}
