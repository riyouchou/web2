package com.yx.pass.remote.pms.model.resp;

import com.yx.pass.remote.pms.model.resp.region.RegionInfoResp;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class DashboardRegionServerResp implements Serializable {

    private static final long serialVersionUID = 5146908224397682434L;

    private Integer availableContainers;

    private Integer totalLocations;

    private List<RegionInfoResp> regions;

    private List<Map<String, String>> gpuTypes;
}

