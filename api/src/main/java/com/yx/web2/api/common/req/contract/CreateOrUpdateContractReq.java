package com.yx.web2.api.common.req.contract;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class CreateOrUpdateContractReq implements Serializable {
    private String contractId;
    private Long contractTenantId;
    private String contractTenantName;
    private String customerLegalEntityName;
    private String customerRegistrationNumber;
    private String customerLegalEntityAddress;
    private String signerName;
    private String signerEmail;
    private String startedTime;
    private Integer freeServiceTermDays;
    private Integer serviceDuration;
    private Integer serviceDurationPeriod;
    private String prePaymentPrice;
    private String customerCountry;
    private List<ContractDevice> devices;

    @Getter
    @Setter
    public static class ContractDevice {
        private String regionCode;
        private String deployRegionCode;
        private String bandWidth;
        private String cpuInfo;
        private String gpuInfo;
        private String mem;
        private String disk;
        private Integer quantity;
        private String unitPrice;
        private String discountPrice;
        private String spec;
        private String deviceInfo;
    }

}
