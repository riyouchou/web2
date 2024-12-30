package com.yx.web2.api.service.servers;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.pass.remote.pms.model.resp.DashboardRegionServerResp;
import com.yx.pass.remote.pms.model.resp.region.RegionInfoResp;
import com.yx.pass.remote.pms.model.resp.resource.PmsOrderContainerInfoResp;
import com.yx.pass.remote.pms.model.resp.resource.PmsServersOrderContainerResp;
import com.yx.pass.remote.pms.model.resp.sepc.OrderResourceSurveyResp;
import com.yx.pass.remote.pms.model.resp.sepc.SpecPriceResp;
import com.yx.pass.remote.pms.model.resp.servers.ServerRegionAndGpuTypeResp;
import com.yx.pass.remote.pms.model.resp.servers.ServerSysInfoResp;
import com.yx.pass.remote.pms.model.resp.servers.ServersContainerResp;
import com.yx.pass.remote.pms.model.resp.servers.page.CustomPage;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.container.dto.FeeApiDTO;
import org.yx.lib.utils.util.R;

import java.util.List;
import java.util.Objects;

public interface ServersService {

    R<Page<PmsServersOrderContainerResp>> page(Integer current, Integer size, String region, String gpu, String resourcePool, AccountModel accountModel);

    R<ServerSysInfoResp> orderDetail(Long id, String resourcePool, AccountModel accountModel);

    R<List<ServerRegionAndGpuTypeResp>> findGpuTypes(String resourcePool, AccountModel accountModel);

    R<List<ServerRegionAndGpuTypeResp>> findRegions(String gpuType, String resourcePool, AccountModel accountModel);

    R<CustomPage<ServersContainerResp>> serversResources(Integer current, Integer size, String regionCode, String gpuType, Integer cards, String resourcePool, AccountModel accountModel);

    R<List<Long>> serversFindCards(String regionCode, String gpuType, String resourcePool, AccountModel accountModel);
}
