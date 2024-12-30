package com.yx.web2.api.common.req.order;

import com.yx.web2.api.common.req.PageReq;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderQueryReq extends PageReq {
    private Integer orderStatus;
    private Integer paymentStatus;
    private String accountName;
    private Long bdAccountId;
    private String bizType = "ARS";
}
