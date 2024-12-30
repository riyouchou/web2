package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderPaymentStatus {

    /**
     * 未支付
     */
    None("None", 0),
    /**
     * 用户已支付首付款，按月正常支付资源费用（用户所绑定支付方式正常）
     */
    NormalPayment("NormalPayment", 1),
    /**
     * 用户已支付首付款，但是按月费用扣费失败（包含多个月一直扣费失败场景）
     */
    OverduePayment("OverduePayment", 2),

    /**
     * 首付款支付失败
     */
    NormalPaymentFailed("NormalPaymentFailed", 3);

    private final String name;
    private final Integer value;
}
