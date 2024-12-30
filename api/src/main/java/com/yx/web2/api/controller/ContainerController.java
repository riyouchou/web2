package com.yx.web2.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.yx.pass.remote.feecenter.model.req.FeeCenterReqBase;
import com.yx.pass.remote.pms.model.resp.sepc.OrderResourceSurveyResp;
import com.yx.pass.remote.pms.model.resp.DashboardRegionServerResp;
import com.yx.pass.remote.pms.model.resp.region.RegionInfoResp;
import com.yx.pass.remote.pms.model.resp.sepc.SpecPriceResp;
import com.yx.web2.api.common.constant.RequestAttributeConstants;
import com.yx.web2.api.common.http.HttpUtils;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.container.dto.FeeApiDTO;
import com.yx.web2.api.service.container.ContainerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;
import org.yx.lib.utils.util.R;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * packageName com.yx.web2.api.controller
 * Dashboard 缺省页显示
 *
 * @author YI-JIAN-ZHANG
 * @className DashboardController
 * @date 2024/9/4
 */
@RefreshScope
@RestController
@RequestMapping("/container")
@RequiredArgsConstructor
public class ContainerController {
    @Resource
    private ContainerService containerService;


    /**
     * DashBoard 缺省页统计数据
     *
     * @return R
     */
    @GetMapping("/dashboard")
    public R<DashboardRegionServerResp> dashboard(@RequestParam(defaultValue = "ARS") String resourcePool,
                                                  @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return containerService.dashboard(resourcePool, accountModel);
    }

    /**
     * 获取计价区域列表【返回只有容器的计价区域】
     *
     * @return R
     */
    @GetMapping("/region/list")
    public R<List<RegionInfoResp>> regionList(@RequestParam(defaultValue = "ARS") String resourcePool,
                                              @RequestParam(defaultValue = "1") Integer isPriceRegion) {

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("resourcePool", resourcePool);
        jsonObject.put("isPriceRegion", isPriceRegion);
        return R.ok(containerService.getRegionList(jsonObject));
    }

    /**
     * 获取指定区域的容器规格列表
     *
     * @param osType 操作系统类型  PC / Mobile
     * @return R
     */
    @GetMapping("/spec/list")
    public R<List<SpecPriceResp>> specList(@RequestParam String osType,
                                           @RequestParam String regionCode) {
        return containerService.specList(osType, regionCode);
    }

    /**
     * 获取指定区域的容器规格概况列表
     *
     * @return R
     */
    @PostMapping("/list")
    public R<List<OrderResourceSurveyResp>> list(@RequestBody FeeApiDTO dto,
                                                 @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel
    ) {

        return containerService.containerList(dto, accountModel);
    }

    /**
     * 获取指定区域的容器规格内订单列表
     *
     * @return R
     */
    @PostMapping("/order/page")
    public R<?> orderContainerList(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody FeeApiDTO dto) {
        return R.ok(containerService.orderContainerList(tenantId, accountModel, dto));
    }

    /**
     * 判断当前用户是否存在订单数据
     *
     * @return R
     */
    @PostMapping("/order/exists")
    public R<?> containerOrderExists(
            @RequestBody FeeApiDTO dto,
                                     @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                                     @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return R.ok(containerService.containerOrderExists(dto.getResourcePool(),tenantId, accountModel).getData());
    }

    /**
     * 服务中订单部署规格列表
     *
     * @return R
     */
    @GetMapping("/deploy/spec/list")
    public R<?> containerOrderDeploySpec(@RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                                         @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
                                         @RequestParam(required = false) Long qTid,
                                         @RequestParam(required = false) String spec,
                                         @RequestParam(required = false) String targetVersion,
                                         @RequestParam(required = false) Long appId
    ) {
        return containerService.containerOrderDeploySpec(tenantId, accountModel, qTid, spec, targetVersion, appId);
    }
}
