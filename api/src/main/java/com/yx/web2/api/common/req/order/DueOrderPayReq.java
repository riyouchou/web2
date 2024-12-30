package com.yx.web2.api.common.req.order;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DueOrderPayReq {
    private String orderId;
    private List<String> paymentOrderIds;
}
