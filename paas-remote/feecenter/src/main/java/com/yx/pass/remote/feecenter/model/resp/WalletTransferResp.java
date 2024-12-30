package com.yx.pass.remote.feecenter.model.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * feeCenter 钱包转账响应信息
 */
@Getter
@Setter
public class WalletTransferResp {

    /**
     * 转账业务单ID
     */
    @JsonProperty("bill_id")
    private Integer billId;
    /**
     * 转账金额
     */
    private String transValue;
    private String from;
    private String to;
    /**
     * 转账时间戳，单位：秒
     */
    private Long transAt;
}
