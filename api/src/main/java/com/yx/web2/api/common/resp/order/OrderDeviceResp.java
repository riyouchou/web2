package com.yx.web2.api.common.resp.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderDeviceResp {
    private Integer orderStatus;

    private String orderId;

    private String spec;

    private String regionCode;

    private Integer quantity;

}
