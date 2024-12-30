package com.yx.web2.api.common.resp.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class OrderDetailResp {
    private String orderId;
    private Integer orderStatus;
    private Integer paymentStatus;
    private String accountName;
    private Long accountId;
    private String bdName;
    private String createTime;
    private Integer serviceDuration;
    private Integer serviceDurationPeriod;
    private String initialPrice;
    private String currentPrice;
    private String discountPrice;
    private String prePaymentPrice;
    private Boolean published;
    private Boolean paid;
    private String serviceTerm;
    private String monthlyPayment;
    private Integer monthlyPaymentCycle;
    private String reason;
    private List<OrderDevice> devices;

    @Getter
    @Setter
    @Builder
    public static class OrderDevice {
        private String regionCode;
        private String regionName;
        private String spec;
        private String specName;
        private String gpuInfo;
        private String cpuInfo;
        private String mem;
        private String disk;
        private String unitPrice;
        private String discountPrice;
        private Integer quantity;
        private String deviceInfo;
    }
}
