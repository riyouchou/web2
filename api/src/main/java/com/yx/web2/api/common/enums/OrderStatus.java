package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {

    /**
     * 用户提交资源申请至财务完成价格确认，订单为该状态
     */
    PendingReview("PendingReview", 1),
    /**
     * 财务确认价格至确认预付款到账，订单为该状态
     */
    WaitingPayment("WaitingPayment", 2),
    /**
     * 等待款项到账确认，用户侧付款完成至财务确认款项到账，订单为该状态
     */
    AwaitingPaymentReceipt("AwaitingPaymentReceipt", 3),
    /**
     * 支付完成至财务确认首付款到账
     */
    NotStarted("NotStarted", 4),
    /**
     * 用户成功支付首付款，平台绑定资源成功，订单为该状态
     */
    InService("InService", 5),
    /**
     * 订单到达结束时间，资源解绑，订单为该状态
     */
    End("End", 6),
    /**
     * 订单资源绑定失败
     */
    Failed("Failed", 7),
    /**
     * BD中止订单、订单所绑定资源均不可用
     */
    Terminated("Terminated", 8),
    /**
     * 取消
     */
    Cancel("Cancel", 9),
    /**
     * 订单异常关闭
     */
    AbnormalCancel("AbnormalCancel", 10),
    /**
     * 信用不足强行终止
     */
    InsufficientCredit("InsufficientCredit", 11)
    ;

    private final String name;
    private final Integer value;
}
