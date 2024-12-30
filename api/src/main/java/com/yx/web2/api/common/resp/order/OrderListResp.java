package com.yx.web2.api.common.resp.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderListResp {
    private String orderId;
    private Integer orderStatus;
    private Integer paymentStatus;
    private String accountName;
    private Long accountId;
    private String bdName;
    private String createTime;
    private String initialPrice;
    private String currentPrice;
    private String prePaymentPrice;
    private Boolean published;
    private Boolean paid;
    private String serviceTerm;
    private Integer orderResourcePool;
    private String reason;
}
