package com.yx.web2.api.service.container;

import com.alibaba.fastjson.JSONObject;
import com.yx.pass.remote.pms.model.resp.resource.PmsOrderContainerInfoResp;
import com.yx.pass.remote.pms.model.resp.sepc.OrderResourceSurveyResp;
import com.yx.pass.remote.pms.model.resp.DashboardRegionServerResp;
import com.yx.pass.remote.pms.model.resp.region.RegionInfoResp;
import com.yx.pass.remote.pms.model.resp.sepc.SpecPriceResp;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.container.dto.FeeApiDTO;
import org.yx.lib.utils.util.R;

import java.util.List;
import java.util.Map;

public interface ContainerService {

    R<RegionInfoResp> getRegionInfo(FeeApiDTO dto);

    /**
     * 查询所有有容器区域列表
     * @param jsonObject  传递参数
     * @author yijian
     * @date 2024/9/19 15:03
     */
    List<RegionInfoResp> getRegionList(JSONObject jsonObject);

    /**
     * Get container dashboard information
     * @author yijian
     * @date 2024/9/19 17:55
     */
    R<DashboardRegionServerResp> dashboard(String resourcePool, AccountModel accountModel);


    R<List<SpecPriceResp>> specList(String osType, String regionCode);

    R<List<OrderResourceSurveyResp>> containerList(FeeApiDTO dto, AccountModel accountModel);

    /**
     * 查询当前租户是否存在订单容器数据
     *
     * @param feeCenterReqBase
     * @param resourcePool
     * @author yijian
     * @date 2024/9/21 16:31
     */
    R<?> containerOrderExists(String resourcePool, Long tenantId, AccountModel accountModel);


    PmsOrderContainerInfoResp orderContainerList(Long tenantId, AccountModel accountModel, FeeApiDTO dto);

    R<?> containerOrderDeploySpec(Long tenantId, AccountModel accountModel, Long qTid, String spec, String targetVersion, Long appId);


    R<Map<String,Object>> availableGpuList(String resourcePool, AccountModel accountModel, String scene);
}
