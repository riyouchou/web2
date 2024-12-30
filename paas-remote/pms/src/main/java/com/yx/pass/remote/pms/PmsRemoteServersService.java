package com.yx.pass.remote.pms;

import cn.hutool.core.util.URLUtil;
import com.alibaba.druid.stat.JdbcSqlStat;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.pass.remote.pms.config.PmsApiConfig;
import com.yx.pass.remote.pms.model.req.OrderResourceSurveyReq;
import com.yx.pass.remote.pms.model.req.PmsOrderContainerReq;
import com.yx.pass.remote.pms.model.resp.resource.PmsOrderContainerInfoResp;
import com.yx.pass.remote.pms.model.resp.resource.PmsServersOrderContainerResp;
import com.yx.pass.remote.pms.model.resp.sepc.OrderResourceSurveyResp;
import com.yx.pass.remote.pms.model.resp.servers.ServerRegionAndGpuTypeResp;
import com.yx.pass.remote.pms.model.resp.servers.ServerSysInfoResp;
import com.yx.pass.remote.pms.model.resp.servers.ServersContainerResp;
import com.yx.pass.remote.pms.model.resp.servers.page.CustomPage;
import com.yx.pass.remote.pms.model.resp.tenant.TenantInfoResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.SpringContextHolder;
import org.yx.lib.utils.util.StringPool;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pms Servers service
 */
public class PmsRemoteServersService extends PmsRemoteService {

    public PmsRemoteServersService(RestTemplate restTemplate, PmsApiConfig pmsApiConfig) {
        super(restTemplate, pmsApiConfig);
    }

    public R<Page<PmsServersOrderContainerResp>> getServersPage(Integer current, Integer size, String region, String gpu, String resourcePool, Long tenantId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("current", current);
        jsonObject.put("size", size);
        jsonObject.put("regionCode", region);
        jsonObject.put("gpuType", gpu);
        jsonObject.put("resourcePool", resourcePool);
        jsonObject.put("wholeSaleTid", tenantId);
        String query = URLUtil.buildQuery(jsonObject, StandardCharsets.UTF_8).replace("%20", " ");
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsServersPageUrl() + StringPool.QUESTION_MARK + query, jsonObject);
        KvLogger.instance(this)
                .p("method", " get PMS-Api Push the current user's BM servers list information")
                .p("resBody", JSONObject.toJSONString(JSON.parseObject(responseEntity.getBody(), new TypeReference<R<Page<PmsServersOrderContainerResp>>>() {
                }.getType())))
                .p("reqBody", jsonObject)
                .p("reqUrl", pmsApiConfig.getPmsServersPageUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<Page<PmsServersOrderContainerResp>>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }

    public R<ServerSysInfoResp> orderDetail(Long id, String resourcePool, Long tenantId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("resourcePool", resourcePool);
        jsonObject.put("tid", tenantId);
        String query = URLUtil.buildQuery(jsonObject, StandardCharsets.UTF_8);
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsServersDetailUrl() + StringPool.QUESTION_MARK + query, jsonObject);
        KvLogger.instance(this)
                .p("method", " get PMS-Api Push the current user's servers Detail information")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<ServerSysInfoResp>>() {
                }.getType()))
                .p("reqBody", jsonObject)
                .p("reqUrl", pmsApiConfig.getPmsServersDetailUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<ServerSysInfoResp>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }

    public R<List<ServerRegionAndGpuTypeResp>> findGpuTypesOrRegions(String gpuType, String resourcePool, Long tenantId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("resourcePool", resourcePool);
        jsonObject.put("gpuType", gpuType);
        jsonObject.put("tid", tenantId);
        String query = URLUtil.buildQuery(jsonObject, StandardCharsets.UTF_8).replace("%20", " ");
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsServersFindGpuOrRegionUrl() + StringPool.QUESTION_MARK + query, jsonObject);
        KvLogger.instance(this)
                .p("method", " Get PMS-Api Push Available GPUTypes Or Regions")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<List<ServerRegionAndGpuTypeResp>>>() {
                }.getType()))
                .p("reqBody", jsonObject)
                .p("reqUrl", pmsApiConfig.getPmsServersFindGpuOrRegionUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<List<ServerRegionAndGpuTypeResp>>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }

    public R<CustomPage<ServersContainerResp>> serversResources(Integer current, Integer size, String regionCode, String gpuType, Integer cards, String resourcePool, Long tenantId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("current", current);
        jsonObject.put("size", size);
        jsonObject.put("regionCode", regionCode);
        jsonObject.put("gpuType", gpuType);
        jsonObject.put("cards", cards);
        jsonObject.put("resourcePool", resourcePool);
//        jsonObject.put("wholeSaleTid", tenantId);
        String query = URLUtil.buildQuery(jsonObject, StandardCharsets.UTF_8).replace("%20", " ");
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsServersResourcesUrl() + StringPool.QUESTION_MARK + query, jsonObject);
        KvLogger.instance(this)
                .p("method", " Get PMS-Api Push Available Servers Resources")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<CustomPage<ServersContainerResp>>>() {
                }.getType()))
                .p("reqBody", jsonObject)
                .p("reqUrl", pmsApiConfig.getPmsServersResourcesUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<CustomPage<ServersContainerResp>>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }

    public R<List<Long>> serversFindCards(String regionCode, String gpuType, String resourcePool, Long tenantId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("regionCode", regionCode);
        jsonObject.put("gpuType", gpuType);
        jsonObject.put("resourcePool", resourcePool);
        String query = URLUtil.buildQuery(jsonObject, StandardCharsets.UTF_8);
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsApiServersCardsUrl() + StringPool.QUESTION_MARK + query, jsonObject);
        KvLogger.instance(this)
                .p("method", " Get PMS-Api Push Available Servers Cards")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<List<Long>>>() {
                }.getType()))
                .p("reqBody", jsonObject)
                .p("reqUrl", pmsApiConfig.getPmsApiServersCardsUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<List<Long>>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }
}
