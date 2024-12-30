package com.yx.web2.api.common.resp.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BillListResp {
    private String orderId;
    private String paymentOrderId;
    private Boolean prePayment;
    private Integer paymentStatus;
    private String payTime;
    private Integer instalmentMonth;
    private Integer instalmentMonthTotal;
    private String amount;
    private Integer orderResourcePool;
}
