package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分期付款的订单状态
 */
@Getter
@AllArgsConstructor
public enum InstalmentPaymentStatus {

    /**
     * 未支付
     */
    None(0),
    /**
     * 已支付
     */
    PAID(1),
    /**
     * 支付失败
     */
    FAILED(2),
    /**
     * 支付取消
     */
    CANCEL(3);

    private final Integer value;
}
