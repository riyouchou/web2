package com.yx.web2.api.common.resp.contract;

import com.yx.web2.api.common.req.contract.CreateOrUpdateContractReq;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ContractDetailResp {
    private String contractId;
    private Integer contractStatus;
    private Long contractTenantId;
    private String contractTenantName;
    private String customerLegalEntityName;
    private String customerRegistrationNumber;
    private String customerLegalEntityAddress;
    private String signerName;
    private String signerEmail;
    private String startedTime;
    private Integer freeServiceTermDays;
    private String amount;
    private String prePaymentPrice;
    private Integer serviceDuration;
    private Integer serviceDurationPeriod;
    private String customerCountry;
    private List<ContractDetailResp.ContractDevice> devices;

    @Getter
    @Setter
    public static class ContractDevice {
        private Long id;
        private String regionCode;
        private String deployRegionCode;
        private String regionName;
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
