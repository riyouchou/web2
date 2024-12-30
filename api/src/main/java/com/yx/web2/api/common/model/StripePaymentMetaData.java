package com.yx.web2.api.common.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class StripePaymentMetaData {

    private List<OrderIdInfo> metaData;

    @Getter
    @Setter
    public static class OrderIdInfo {
        private Long oid;
        private List<Long> pIds;
    }
}
