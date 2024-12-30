package com.yx.web2.api.common.req.order;

import lombok.Getter;
import lombok.Setter;
import org.yx.lib.utils.util.StringUtil;

import java.util.List;

import static org.yx.lib.utils.util.StringUtil.isNotBlank;

@Getter
@Setter
public class OrderPayVirtualReq {
    private List<OrderPay> orderPayList;
    private String from;
    private String to;
    private String amount;
    private Integer type;
    private String hashCode;

    @Getter
    @Setter
    public static class OrderPay {
        private String orderId;
        private List<String> orderPaymentIdList;

        public boolean isValid() {
            return isNotBlank(orderId) &&
                    isValidOrderPaymentIdList(orderPaymentIdList);
        }

        private static boolean isValidOrderPaymentIdList(List<String> list) {
            if (list == null || list.isEmpty()) {
                return false;
            }
            // Check that all elements in the list are not blank.
            return list.stream().allMatch(StringUtil::isNotBlank);
        }
    }


    public boolean isValidPre() {
        if (orderPayList == null || orderPayList.isEmpty() || !orderPayList.stream().allMatch(OrderPay::isValid)) {
            return false;
        }
        return isNotBlank(from) &&
                isNotBlank(to) &&
                isNotBlank(amount) &&
                type != null;
    }

    public boolean isValid() {
        if (orderPayList == null || orderPayList.isEmpty() || !orderPayList.stream().allMatch(OrderPay::isValid)) {
            return false;
        }
        return isNotBlank(from) &&
                isNotBlank(to) &&
                isNotBlank(amount) &&
                type != null &&
                isNotBlank(hashCode);
    }

}
