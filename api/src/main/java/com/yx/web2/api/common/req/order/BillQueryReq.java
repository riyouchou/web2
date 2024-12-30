package com.yx.web2.api.common.req.order;

import com.yx.web2.api.common.req.PageReq;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BillQueryReq extends PageReq {
    private String startTime;
    private String endTime;
    private Integer paymentStatus;
    private String orderId;
    private String paymentOrderId;
    /**
     *default 1 1. ARS 2 BM
     *
     */
    private Integer orderResourcePool = 1;
}
