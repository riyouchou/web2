package com.yx.web2.api.common.web3api;

import cn.hutool.core.map.MapBuilder;
import com.google.common.collect.Maps;
import com.yx.pass.remote.pms.PmsRemoteRegionService;
import com.yx.pass.remote.pms.PmsRemoteSpecService;
import com.yx.pass.remote.pms.model.resp.region.RegionInfoResp;
import com.yx.pass.remote.pms.model.resp.sepc.ConfigContainerSpecResp;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringUtil;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class Web3BasicDataService {

    private final PmsRemoteSpecService pmsRemoteSpecService;
    private final PmsRemoteRegionService pmsRemoteRegionService;
    private static final Map<String, String> REGION_CACHE_DATA = Maps.newConcurrentMap();
    private static final Map<String, SpecCacheInfo> SPEC_CACHE_DATA = Maps.newConcurrentMap();

    public String getRegion(String regionCode) {
        String regionName = REGION_CACHE_DATA.get(regionCode);
        if (StringUtil.isNotBlank(regionName)) {
            return regionName;
        }
        R<RegionInfoResp> regionInfoRespR = pmsRemoteRegionService.getRegionInfo(regionCode);
        if (regionInfoRespR.getCode() != 0) {
            return null;
        }
        regionName = regionInfoRespR.getData().getRegionName();
        REGION_CACHE_DATA.put(regionCode, regionName);
        return regionName;
    }

    public SpecCacheInfo getSpec(String specCode) {
        SpecCacheInfo specCacheInfo = SPEC_CACHE_DATA.get(specCode);
        if (specCacheInfo != null) {
            return specCacheInfo;
        }
        R<ConfigContainerSpecResp> specRespR = pmsRemoteSpecService.getSpecInfo(specCode);
        if (specRespR.getCode() != 0) {
            return null;
        }
        specCacheInfo = SpecCacheInfo.builder()
                .specCode(specCode)
                .specName(specRespR.getData().getSpecName())
                .cpu(specRespR.getData().getCpu())
                .gpu(specRespR.getData().getGpu())
                .mem(specRespR.getData().getMem())
                .disk(specRespR.getData().getDisk())
                .build();
        SPEC_CACHE_DATA.put(specCode, specCacheInfo);
        return specCacheInfo;
    }

    @PreDestroy
    public void destroy() {
        REGION_CACHE_DATA.clear();
        SPEC_CACHE_DATA.clear();
    }

    @Getter
    @Builder
    public static class SpecCacheInfo {
        String specCode;
        String specName;
        String gpu;
        String cpu;
        String mem;
        String disk;
    }
}
