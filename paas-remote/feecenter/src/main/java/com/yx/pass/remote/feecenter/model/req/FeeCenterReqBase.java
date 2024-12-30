package com.yx.pass.remote.feecenter.model.req;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class FeeCenterReqBase {

    /**
     * 租户Id
     */
    private Long tid;

    /**
     * 账户Id
     */
    private Long accountId;

    /**
     * 租户类型
     */
    private String tenantType;

    private String resourcePool;
}
