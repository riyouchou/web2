package com.yx.web2.api.service.container.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.yx.pass.remote.feecenter.FeeCenterRemoteOrderService;
import com.yx.pass.remote.feecenter.model.req.FeeCenterReqBase;
import com.yx.pass.remote.pms.PmsRemoteContainerOrderService;
import com.yx.pass.remote.pms.PmsRemoteRegionService;
import com.yx.pass.remote.pms.PmsRemoteSpecService;
import com.yx.pass.remote.pms.model.req.OrderResourceSurveyReq;
import com.yx.pass.remote.pms.model.req.PmsOrderContainerReq;
import com.yx.pass.remote.pms.model.resp.DashboardRegionServerResp;
import com.yx.pass.remote.pms.model.resp.region.RegionInfoResp;
import com.yx.pass.remote.pms.model.resp.resource.PmsOrderContainerInfoResp;
import com.yx.pass.remote.pms.model.resp.resource.PmsOrderContainerPageResp;
import com.yx.pass.remote.pms.model.resp.sepc.OrderResourceSurveyResp;
import com.yx.pass.remote.pms.model.resp.sepc.SpecPriceResp;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.container.dto.FeeApiDTO;
import com.yx.web2.api.common.req.games.SpecGameDto;
import com.yx.web2.api.config.Web2ApiConfig;
import com.yx.web2.api.entity.AthOrderInfoEntity;
import com.yx.web2.api.service.IAthOrderInfoService;
import com.yx.web2.api.service.IOrderPaymentService;
import com.yx.web2.api.service.IOrderService;
import com.yx.web2.api.service.ISubscribedServiceService;
import com.yx.web2.api.service.container.ContainerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.glassfish.jersey.internal.guava.Maps;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yx.lib.utils.util.R;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.yx.web2.api.common.constant.Web2ApiConstants.SCENE_REGION;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerServiceImpl implements ContainerService {
    private String dashboardCacheKey = "dashboardCache:%s";
    private String regionCacheKey = "regionCache:%s";
    private final Cache<String, DashboardRegionServerResp> dashboardCache = CacheBuilder.newBuilder().expireAfterWrite(RandomUtils.nextInt(15, 25), TimeUnit.SECONDS).build();

    private final Cache<String, List<RegionInfoResp>> availableRegionCache = CacheBuilder.newBuilder().expireAfterWrite(RandomUtils.nextInt(30, 45), TimeUnit.SECONDS).build();

    private final PmsRemoteRegionService pmsRemoteRegionService;
    private final PmsRemoteSpecService pmsRemoteSpecService;
    private final PmsRemoteContainerOrderService pmsRemoteContainerOrderService;
    private final FeeCenterRemoteOrderService feeCenterRemoteOrderService;
    private final Web2ApiConfig web2ApiConfig;
    private final IOrderPaymentService iOrderPaymentService;
    private final IOrderService iOrderService;
    private final IAthOrderInfoService iAthOrderInfoService;
    private final ISubscribedServiceService iSubscribedServiceService;

    @Override
    public R<RegionInfoResp> getRegionInfo(FeeApiDTO dto) {
        return pmsRemoteRegionService.getRegionInfo(dto.getRegion());
    }

    @Override
    public List<RegionInfoResp> getRegionList(JSONObject jsonObject) {
        return Optional.ofNullable(availableRegionCache.getIfPresent(regionCacheKey)).orElseGet(() -> {
            List<RegionInfoResp> data = pmsRemoteRegionService.getRegionList(jsonObject).getData();
            data.parallelStream().filter(x -> x.getAvailableContainers() > 0).collect(Collectors.toList());
            availableRegionCache.put(regionCacheKey, data);
            return data;
        });
    }

    @Override
    public R<DashboardRegionServerResp> dashboard(String resourcePool, AccountModel accountModel) {
        return R.ok(Optional.ofNullable(dashboardCache.getIfPresent(String.format(dashboardCacheKey, resourcePool))).orElseGet(() -> {
            DashboardRegionServerResp data = pmsRemoteRegionService.dashboard(resourcePool, NumberUtils.LONG_ZERO, "dashboard").getData();
            dashboardCache.put(String.format(dashboardCacheKey, resourcePool), data);
            return data;
        }));
    }

    @Override
    public R<List<SpecPriceResp>> specList(String osType, String regionCode) {
        String adaptableGames = web2ApiConfig.getAdaptableGames();
        List<SpecGameDto> games = JSONObject.parseObject(adaptableGames, new TypeReference<List<SpecGameDto>>() {
        });
        List<SpecPriceResp> specPriceResp = pmsRemoteSpecService.specList(osType, regionCode).getData();
        if (!CollectionUtils.isEmpty(specPriceResp)) {
            specPriceResp = specPriceResp.stream().peek(x -> {
                x.setWholesalePrice(x.getWholesalePrice().multiply(BigDecimal.valueOf(web2ApiConfig.getUnitPriceCoefficient())));
                x.setAdaptableGames(games.parallelStream().filter(mapping -> x.getSpec().equals(mapping.getSpec())).findFirst().map(SpecGameDto::getGames).orElse(new ArrayList<>()));
            }).filter(x -> x.getAvailable() > 0).collect(Collectors.toList());
        }
        return R.ok(specPriceResp);
    }

    @Override
    public R<List<OrderResourceSurveyResp>> containerList(FeeApiDTO dto, AccountModel accountModel) {
        OrderResourceSurveyReq req = new OrderResourceSurveyReq();
        req.setRegion(dto.getRegion());
        req.setSpec(dto.getSpec());
        req.setResourcePool("ARS");
        List<OrderResourceSurveyResp> resourceSurvey = pmsRemoteContainerOrderService.getOrderResourceSurvey(req, accountModel.getTenantId()).getData();
        if (!CollectionUtils.isEmpty(resourceSurvey)) {
            resourceSurvey = resourceSurvey.stream().filter(x -> x.getRegion().contains("Price")).collect(Collectors.toList());
        }
        return R.ok(resourceSurvey);
    }

    @Override
    public R<?> containerOrderExists(String resourcePool, Long tenantId, AccountModel accountModel) {
        return feeCenterRemoteOrderService.containerOrderExists(FeeCenterReqBase.builder().tid(tenantId).resourcePool(resourcePool).tenantType(accountModel.getTenantType()).accountId(accountModel.getAccountId()).build());
    }

    @Override
    public PmsOrderContainerInfoResp orderContainerList(Long tenantId, AccountModel accountModel, FeeApiDTO dto) {
        PmsOrderContainerReq build = PmsOrderContainerReq.builder().size(dto.getSize()).current(dto.getCurrent()).spec(dto.getSpec()).region(dto.getRegion()).tid(tenantId).build();
        PmsOrderContainerInfoResp data = pmsRemoteContainerOrderService.orderContainerList(build).getData();
        List<PmsOrderContainerPageResp> pages = data.getPages();
        if (!CollectionUtils.isEmpty(pages)) {
            Set<String> orderCodes = pages.parallelStream().map(PmsOrderContainerPageResp::getOrderCode).collect(Collectors.toSet());
            if (!CollectionUtils.isEmpty(orderCodes)) {
                Map<String, String> athOrderByOrderMap = iAthOrderInfoService.list(new LambdaQueryWrapper<AthOrderInfoEntity>().in(AthOrderInfoEntity::getAthOrderId, orderCodes)).parallelStream().collect(Collectors.toMap(AthOrderInfoEntity::getAthOrderId, AthOrderInfoEntity::getOrderId, (key1, key2) -> key2));

                pages.forEach(x -> {
                    x.setOrderCode(athOrderByOrderMap.getOrDefault(x.getOrderCode(), StringUtils.EMPTY));
                    x.setEffectTimeStr(x.getEffectTime() > NumberUtils.LONG_ZERO ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(x.getEffectTime()), ZoneOffset.UTC)) : StringUtils.EMPTY);
                    x.setEndTimeStr(x.getEndTime() > NumberUtils.LONG_ZERO ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.ofInstant(Instant.ofEpochSecond(x.getEndTime()), ZoneOffset.UTC)) : StringUtils.EMPTY);
                });
            }
        }

        return data;
    }

    @Override
    public R<?> containerOrderDeploySpec(Long tenantId, AccountModel accountModel, Long qTid, String spec, String targetVersion, Long appId) {
        return iOrderService.containerOrderDeploySpec(ObjectUtils.isNotEmpty(qTid) ? qTid : tenantId, spec, targetVersion, appId);
    }


    @Override
    public R<Map<String, Object>> availableGpuList(String resourcePool, AccountModel accountModel, String scene) {
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(4);
        DashboardRegionServerResp data = pmsRemoteRegionService.dashboard(resourcePool, accountModel.getTenantId(), scene).getData();
        if (SCENE_REGION.equals(scene)) {
            params.put("regions", CollectionUtils.isEmpty(data.getRegions()) ? new ArrayList<>() : data.getRegions());
        } else {
            params.put("gpuTypes", CollectionUtils.isEmpty(data.getGpuTypes()) ? new ArrayList<>() : data.getGpuTypes());
        }
        return R.ok(params);
    }
}
