package com.yx.web2.api.common.resp.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AthTransferRecordListResp {
    private String accountId;
    private String accountName;
    private String orderId;
    private Integer transferStatus;
    private Integer transferType;
    private String amount;
    private String transferredTime;
}
