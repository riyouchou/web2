package com.yx.web2.api.common.req.order;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class CidsByDeviceInfoReq {

    private String orderId;
    private String wholesaleTid;
    private String cids;
    private List<SpecRegionSysInfo> specRegionSysInfos;

    @Getter
    @Setter
    @Builder
    public static class SpecRegionSysInfo {
        private String gpuCount;
        private String gpuManufacturer;
        private String gpuType;
        private String gpuBusType;
        private String gpuMem;
        private String gpuMemUnit;
        private String cpuNum;
        private String cpuManufacturer;
        private String cpuType;
        private String cpuCores;
        private String cpuCount;
        private String cpuSpeed;
        private String cpuSpeedUnit;
        private String osMem;
        private String storage;
        private String storageUnit;
        private String nic;
        private String nicUnit;
        private String bandwidthLevel;
        private String bandwidthUnit;
        private String spec;
        private String quantity;
        private String regionCode;
        private long orderDeviceId;
        private String resourcePool;
    }
}