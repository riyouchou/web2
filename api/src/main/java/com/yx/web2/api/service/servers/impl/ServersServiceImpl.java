package com.yx.web2.api.service.servers.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.pass.remote.pms.PmsRemoteContainerOrderService;
import com.yx.pass.remote.pms.PmsRemoteServersService;
import com.yx.pass.remote.pms.model.resp.resource.PmsServersOrderContainerResp;
import com.yx.pass.remote.pms.model.resp.servers.ServerRegionAndGpuTypeResp;
import com.yx.pass.remote.pms.model.resp.servers.ServerSysInfoResp;
import com.yx.pass.remote.pms.model.resp.servers.ServersContainerResp;
import com.yx.pass.remote.pms.model.resp.servers.page.CustomPage;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.config.Web2ApiConfig;
import com.yx.web2.api.entity.SubscribedServiceEntity;
import com.yx.web2.api.service.ISubscribedServiceService;
import com.yx.web2.api.service.servers.ServersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.util.R;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServersServiceImpl implements ServersService {
    private final PmsRemoteServersService pmsRemoteServersService;
    private final ISubscribedServiceService iSubscribedServiceService;
    private final Web2ApiConfig web2ApiConfig;


    @Override
    public R<Page<PmsServersOrderContainerResp>> page(Integer current, Integer size, String region, String gpu, String resourcePool, AccountModel accountModel) {
        R<Page<PmsServersOrderContainerResp>> page = pmsRemoteServersService.getServersPage(current, size, region, gpu, resourcePool, accountModel.getTenantId());
        if (ObjectUtils.isNotEmpty(page.getData())) {
            List<PmsServersOrderContainerResp> data = page.getData().getRecords();
            if (!CollectionUtils.isEmpty(data)) {
                Set<String> orderCodes = data.parallelStream().map(PmsServersOrderContainerResp::getOrderCode).collect(Collectors.toSet());
                if (!CollectionUtils.isEmpty(orderCodes)) {
                    Map<String, SubscribedServiceEntity> subscribedServiceEntityMap = iSubscribedServiceService.
                            list(new LambdaQueryWrapper<SubscribedServiceEntity>().in(SubscribedServiceEntity::getAthOrderId, orderCodes)).parallelStream().collect(Collectors.toMap(SubscribedServiceEntity::getAthOrderId, x -> x, (key1, key2) -> key2));
                    data.forEach(x -> {
                        x.setEffectTime(subscribedServiceEntityMap.getOrDefault(x.getOrderCode(), new SubscribedServiceEntity()).getServiceBeginTime());
                        x.setEndTime(subscribedServiceEntityMap.getOrDefault(x.getOrderCode(), new SubscribedServiceEntity()).getServiceEndTime());
                        if (!ObjectUtils.isEmpty(web2ApiConfig.getGpuManufacturer())) {
                            x.setGpuManufacturer(web2ApiConfig.getGpuManufacturer());
                        }
                    });
                }
                page.getData().setRecords(data);
            }
            return page;
        }
        return page;
    }

    @Override
    public R<ServerSysInfoResp> orderDetail(Long id, String resourcePool, AccountModel accountModel) {
        ServerSysInfoResp data = pmsRemoteServersService.orderDetail(id, resourcePool, accountModel.getTenantId()).getData();
        if (!StringUtils.isEmpty(data.getOrderCode())) {
            Map<String, SubscribedServiceEntity> subscribedServiceEntityMap = iSubscribedServiceService.
                    list(new LambdaQueryWrapper<SubscribedServiceEntity>().eq(SubscribedServiceEntity::getAthOrderId, data.getOrderCode()))
                    .parallelStream().collect(Collectors.toMap(SubscribedServiceEntity::getAthOrderId, x -> x, (key1, key2) -> key2));
            data.setEffectTime(subscribedServiceEntityMap.get(data.getOrderCode()).getServiceBeginTime());
            data.setEndTime(subscribedServiceEntityMap.get(data.getOrderCode()).getServiceEndTime());
            data.setOrderCode(subscribedServiceEntityMap.get(data.getOrderCode()).getOrderId());
            if (!ObjectUtils.isEmpty(web2ApiConfig.getGpuManufacturer())) {
                data.setGpuManufacturer(web2ApiConfig.getGpuManufacturer());
            }
        }

        return R.ok(data);
    }

    @Override
    public R<List<ServerRegionAndGpuTypeResp>> findGpuTypes(String resourcePool, AccountModel accountModel) {
        List<ServerRegionAndGpuTypeResp> data = pmsRemoteServersService.findGpuTypesOrRegions(StringUtils.EMPTY, resourcePool, accountModel.getTenantId()).getData();
        data.parallelStream().forEach(x -> {
                if (!ObjectUtils.isEmpty(web2ApiConfig.getGpuManufacturer())) {
                    x.setGpuManufacturer(web2ApiConfig.getGpuManufacturer());
                }
            }
        );
        Collections.sort(data, new Comparator<ServerRegionAndGpuTypeResp>() {
            @Override
            public int compare(ServerRegionAndGpuTypeResp o1, ServerRegionAndGpuTypeResp o2) {
                String gpuType1 = o1.getGpuType();
                String gpuType2 = o2.getGpuType();

                if ("H100".equals(gpuType1)) return -1;
                if ("H100".equals(gpuType2)) return 1;

                return customCompare(gpuType1, gpuType2);
            }
        });


        return R.ok(data);
    }

    private static int customCompare(String o1, String o2) {
        char c1 = o1.charAt(0);
        char c2 = o2.charAt(0);

        int priority1 = Character.isLetter(c1) ? 1 : 2;
        int priority2 = Character.isLetter(c2) ? 1 : 2;

        if (priority1 != priority2) {
            return Integer.compare(priority1, priority2);
        }

        return o1.compareTo(o2);
    }

    @Override
    public R<List<ServerRegionAndGpuTypeResp>> findRegions(String gpuType, String resourcePool, AccountModel accountModel) {
        List<ServerRegionAndGpuTypeResp> data = pmsRemoteServersService.findGpuTypesOrRegions(gpuType, resourcePool, accountModel.getTenantId()).getData();
        data.sort(Comparator.comparing(ServerRegionAndGpuTypeResp::getRegionName));
        return R.ok(data);
    }

    @Override
    public R<CustomPage<ServersContainerResp>> serversResources(Integer current, Integer size, String regionCode, String gpuType, Integer cards, String resourcePool, AccountModel accountModel) {
        CustomPage<ServersContainerResp> data = pmsRemoteServersService.serversResources(current, size, regionCode, gpuType, cards, resourcePool, accountModel.getTenantId()).getData();
        data.getRecords().forEach(x ->{
            x.setPrice(x.getPrice().multiply(BigDecimal.valueOf(web2ApiConfig.getUnitPriceCoefficient())));
            if (!ObjectUtils.isEmpty(web2ApiConfig.getGpuManufacturer())) {
                x.setGpuManufacturer(web2ApiConfig.getGpuManufacturer());
            }
        }
        );
        return R.ok(data);
    }

    @Override
    public R<List<Long>> serversFindCards(String regionCode, String gpuType, String resourcePool, AccountModel accountModel) {
        List<Long> data = pmsRemoteServersService.serversFindCards(regionCode, gpuType, resourcePool, accountModel.getTenantId()).getData();
        return R.ok(data);
    }

}
