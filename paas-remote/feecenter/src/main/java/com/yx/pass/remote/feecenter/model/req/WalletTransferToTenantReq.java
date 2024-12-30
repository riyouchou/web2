package com.yx.pass.remote.feecenter.model.req;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * feeCenter 钱包转账请求信息
 */
@Getter
@SuperBuilder
public class WalletTransferToTenantReq extends FeeCenterReqBase {
    /**
     * 转账出账租户Id
     */
    private Integer fromTid;
    /**
     * 转账入账租户Id
     */
    private Integer toTid;
    /**
     * 转账金额
     */
    private String amount;
}
