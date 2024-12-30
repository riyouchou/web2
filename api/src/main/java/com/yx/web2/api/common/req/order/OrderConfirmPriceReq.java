package com.yx.web2.api.common.req.order;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderConfirmPriceReq {
    private String orderId;
    private String confirmPrePaymentPrice;
    private String confirmTotalPrice;
}
