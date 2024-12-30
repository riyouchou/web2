package com.yx.pass.remote.feecenter.model.req;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * feeCenter 钱包转账请求信息
 */
@Getter
@SuperBuilder
public class WalletTransferToAdminReq extends FeeCenterReqBase {
    /**
     * 转账出账租户Id
     */
    private Integer fromTid;

    /**
     * 回滚转账单号
     */
    private Integer billId;
}
