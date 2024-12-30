package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContractStatus {

    /**
     * 合同创建完成，等待财务审核
     */
    PendingApprove("PendingApprove", 1),
    /**
     * 合同创建及审核通过，等待客户签署合同
     */
    PendingSign("PendingSign", 2),
    /**
     * 合同签署完成
     */
    Signed("Signed", 3);

    private final String name;
    private final Integer value;
}
