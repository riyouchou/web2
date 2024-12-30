package com.yx.web2.api.common.req.order;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderPublishPriceReq {
    private String orderId;
    private Boolean publish;
    private String rejectMsg;
}
