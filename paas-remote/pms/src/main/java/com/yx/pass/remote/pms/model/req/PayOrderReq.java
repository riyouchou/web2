package com.yx.pass.remote.pms.model.req;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class PayOrderReq {
    private String orderCode;
    private Long tid;
}
