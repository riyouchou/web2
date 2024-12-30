package com.yx.web2.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.pass.remote.pms.model.resp.DashboardRegionServerResp;
import com.yx.pass.remote.pms.model.resp.region.RegionInfoResp;
import com.yx.pass.remote.pms.model.resp.resource.PmsServersOrderContainerResp;
import com.yx.pass.remote.pms.model.resp.sepc.OrderResourceSurveyResp;
import com.yx.pass.remote.pms.model.resp.sepc.SpecPriceResp;
import com.yx.pass.remote.pms.model.resp.servers.ServerRegionAndGpuTypeResp;
import com.yx.pass.remote.pms.model.resp.servers.ServerSysInfoResp;
import com.yx.pass.remote.pms.model.resp.servers.ServersContainerResp;
import com.yx.pass.remote.pms.model.resp.servers.page.CustomPage;
import com.yx.web2.api.common.constant.RequestAttributeConstants;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.container.dto.FeeApiDTO;
import com.yx.web2.api.service.container.ContainerService;
import com.yx.web2.api.service.servers.ServersService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;
import org.yx.lib.utils.util.R;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

import static com.yx.web2.api.common.constant.Web2ApiConstants.SCENE_GPU;
import static com.yx.web2.api.common.constant.Web2ApiConstants.SCENE_REGION;

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
@RequestMapping("/servers")
@RequiredArgsConstructor
public class ServersController {
    @Resource
    private ContainerService containerService;
    @Resource
    private ServersService serversService;


    /**
     * DashBoard 缺省页统计数据
     *
     * @return R
     */
    @GetMapping("/dashboard")
    public R<DashboardRegionServerResp> dashboard(@RequestParam(defaultValue = "BM") String resourcePool,
                                                  @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return containerService.dashboard(resourcePool, accountModel);
    }

    @GetMapping("/purchased/regions")
    public R<Map<String,Object>> regionList(@RequestParam(defaultValue = "BM") String resourcePool,
                                                   @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return containerService.availableGpuList(resourcePool, accountModel, SCENE_REGION);
    }

    @GetMapping("/purchased/gpuTypes")
    public R<Map<String,Object>> gpuList(@RequestParam(defaultValue = "BM") String resourcePool,
                                         @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return containerService.availableGpuList(resourcePool, accountModel, SCENE_GPU);
    }

    @GetMapping("/order/page")
    public R<Page<PmsServersOrderContainerResp>> page(@RequestParam(defaultValue = "1") Integer current,
                                                      @RequestParam(defaultValue = "10") Integer size,
                                                      @RequestParam String regionCode,
                                                      @RequestParam String gpuType,
                                                      @RequestParam(defaultValue = "BM") String resourcePool,
                                                      @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return serversService.page(current, size, regionCode, gpuType, resourcePool, accountModel);
    }


    @GetMapping("/order/detail")
    public R<ServerSysInfoResp> orderDetail(
            @RequestParam Long id,
            @RequestParam(defaultValue = "BM") String resourcePool,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return serversService.orderDetail(id, resourcePool, accountModel);
    }

    @GetMapping("/find/gpuTypes")
    public R<List<ServerRegionAndGpuTypeResp>> findGpuTypes(
            @RequestParam(defaultValue = "BM") String resourcePool,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return serversService.findGpuTypes(resourcePool, accountModel);
    }

    @GetMapping("/find/regions")
    public R<List<ServerRegionAndGpuTypeResp>> findRegions(
            @RequestParam(defaultValue = "BM") String resourcePool,
            @RequestParam String gpuType,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return serversService.findRegions(gpuType, resourcePool, accountModel);
    }

    @GetMapping("/resources")
    public R<CustomPage<ServersContainerResp>> serversResources(@RequestParam(defaultValue = "1") Integer current,
                                                                @RequestParam(defaultValue = "10") Integer size,
                                                                @RequestParam String regionCode,
                                                                @RequestParam String gpuType,
                                                                @RequestParam(required = false) Integer cards,
                                                                @RequestParam(defaultValue = "BM", required = false) String resourcePool,
                                                                @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return serversService.serversResources(current, size, regionCode, gpuType, cards, resourcePool, accountModel);
    }

    @GetMapping("/find/gpuCards")
    public R<List<Long>> serversFindCards(
            @RequestParam String regionCode,
            @RequestParam String gpuType,
            @RequestParam(defaultValue = "BM") String resourcePool,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return serversService.serversFindCards(regionCode, gpuType, resourcePool, accountModel);
    }

}
