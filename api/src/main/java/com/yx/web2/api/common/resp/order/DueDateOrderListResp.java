package com.yx.web2.api.common.resp.order;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DueDateOrderListResp {
    private String orderId;
    private List<DueDateOrder> dueDateList;

    @Getter
    @Setter
    public static class DueDateOrder {
        private String orderPaymentId;
        private String period;
        private String billingType;
        private String amount;
        private String dueDate;
    }
}
