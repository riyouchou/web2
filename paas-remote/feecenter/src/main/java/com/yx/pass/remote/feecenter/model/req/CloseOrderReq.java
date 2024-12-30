package com.yx.pass.remote.feecenter.model.req;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class CloseOrderReq {
    private String orderCode;
    private Long tid;
}
