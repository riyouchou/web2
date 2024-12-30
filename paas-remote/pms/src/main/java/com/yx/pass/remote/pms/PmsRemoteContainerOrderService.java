package com.yx.pass.remote.pms;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.pass.remote.pms.config.PmsApiConfig;
import com.yx.pass.remote.pms.model.req.OrderResourceSurveyReq;
import com.yx.pass.remote.pms.model.req.PmsOrderContainerReq;
import com.yx.pass.remote.pms.model.resp.resource.PmsOrderContainerInfoResp;
import com.yx.pass.remote.pms.model.resp.resource.PmsServersOrderContainerResp;
import com.yx.pass.remote.pms.model.resp.sepc.OrderResourceSurveyResp;
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
 * Pms 区域相关服务
 */
public class PmsRemoteContainerOrderService extends PmsRemoteService {

    public PmsRemoteContainerOrderService(RestTemplate restTemplate, PmsApiConfig pmsApiConfig) {
        super(restTemplate, pmsApiConfig);
    }

    /**
     * 查询已购资源概况数据列表
     *
     * @param orderResourceSurveyReq feeCenter 查询已购资源概况数据列表
     * @param tenantId 当前登录Tid
     * @return 订单信息
     */
    public R<List<OrderResourceSurveyResp>> getOrderResourceSurvey(OrderResourceSurveyReq orderResourceSurveyReq, Long tenantId) {
        PmsRemoteTenantService tenantService = SpringContextHolder.getBean(PmsRemoteTenantService.class);
        R<TenantInfoResp> tenantInfo = tenantService.getTenantInfo(tenantId);
        if (tenantInfo == null || tenantInfo.getCode() != R.ok().getCode()) {
            return R.failed(404, "not found tid info from pms");
        }
        ResponseEntity<String> responseEntity = postRoute(pmsApiConfig.getOrderResourceSurveyUrl(), JSONObject.toJSONString(orderResourceSurveyReq), tenantInfo.getData().getAkSk().getAk(), tenantInfo.getData().getAkSk().getSk());

        KvLogger.instance(this)
                .p("method", "get PMS push ResourceSurvey information")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<List<OrderResourceSurveyResp>>>() {
                }.getType()))
                .p("reqBody", JSONObject.toJSONString(orderResourceSurveyReq))
                .p("tenantInfo-id", tenantInfo.getData().getId())
                .p("tenantInfo-ak", tenantInfo.getData().getAkSk().getAk())
                .p("tenantInfo-sk", tenantInfo.getData().getAkSk().getSk())
                .p("ReqUrl", pmsApiConfig.getOrderResourceSurveyUrl())
                .i();
        if (responseEntity != null) {
            R<List<OrderResourceSurveyResp>> r = JSON.parseObject(responseEntity.getBody(), new cn.hutool.core.lang.TypeReference<R<List<OrderResourceSurveyResp>>>() {
            }.getType());
            if (r != null && r.getCode() == 0) {
                return r;
            }
        }
        return null;
    }

    @SuppressWarnings("u")
    public R<PmsOrderContainerInfoResp> orderContainerList(PmsOrderContainerReq build) {
        HashMap postBody = JSONObject.parseObject(JSONObject.toJSONString(build), HashMap.class);
        PmsRemoteTenantService tenantService = SpringContextHolder.getBean(PmsRemoteTenantService.class);
        R<TenantInfoResp> tenantInfo = tenantService.getTenantInfo(build.getTid());
        if (tenantInfo == null || tenantInfo.getCode() != R.ok().getCode()) {
            return R.failed(404, "not found tid info from pms");
        }
        ResponseEntity<String> responseEntity = postRoute(pmsApiConfig.getPmsApiOrderListUrl(), postBody, tenantInfo.getData().getAkSk().getAk(), tenantInfo.getData().getAkSk().getSk());

        KvLogger.instance(this)
                .p("method", " get Fee-Idc-Api Push the current user's order list information")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<PmsOrderContainerInfoResp>>() {
                }.getType()))
                .p("reqBody", JSONObject.toJSONString(build))
                .p("tenantInfo-id", tenantInfo.getData().getId())
                .p("tenantInfo-ak", tenantInfo.getData().getAkSk().getAk())
                .p("tenantInfo-sk", tenantInfo.getData().getAkSk().getSk())
                .p("ReqUrl", pmsApiConfig.getPmsApiOrderListUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<PmsOrderContainerInfoResp>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }

    public R<List<Map<String, String>>> getContainerOrderDeployStrategy(Long tid, String targetVersion, String qSpec, Long appId) {
        PmsRemoteTenantService tenantService = SpringContextHolder.getBean(PmsRemoteTenantService.class);
        R<TenantInfoResp> tenantInfo = tenantService.getTenantInfo(tid);
        if (tenantInfo == null || tenantInfo.getCode() != R.ok().getCode()) {
            return R.failed(404, "not found tid info from pms");
        }
        JSONObject params = new JSONObject();
        params.put("tid", tid);
        params.put("targetVersion", targetVersion);
        params.put("resourcePool", "ARS");
        params.put("spec", qSpec);
        params.put("appId", appId);
        ResponseEntity<String> responseEntity = postRoute(pmsApiConfig.getDeployContainerStrategyUrl(), params, tenantInfo.getData().getAkSk().getAk(), tenantInfo.getData().getAkSk().getSk());
        KvLogger.instance(this)
                .p("method", "Get PMS push Container Order Deploy Strategy information")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<List<Map<String, String>>>>() {
                }.getType()))
                .p("reqBody", params)
                .p("tenantInfo-id", tenantInfo.getData().getId())
                .p("tenantInfo-ak", tenantInfo.getData().getAkSk().getAk())
                .p("tenantInfo-sk", tenantInfo.getData().getAkSk().getSk())
                .p("ReqUrl", pmsApiConfig.getDeployContainerStrategyUrl())
                .i();
        if (responseEntity != null) {
            R<List<Map<String, String>>> r = JSON.parseObject(responseEntity.getBody(), new cn.hutool.core.lang.TypeReference<R<List<Map<String, String>>>>() {
            }.getType());
            if (r != null && r.getCode() == 0) {
                return r;
            }
        }
        return null;

    }

    public R<Page<PmsServersOrderContainerResp>> getServersPage(Integer current, Integer size, String region, String gpu, String resourcePool, Long tenantId) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("current", current);
        jsonObject.put("size", size);
        jsonObject.put("region", region);
        jsonObject.put("gpuType", gpu);
        jsonObject.put("resourcePool", resourcePool);
        jsonObject.put("wholeSaleTid", tenantId);
        String query = URLUtil.buildQuery(jsonObject, StandardCharsets.UTF_8);
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsServersPageUrl() + StringPool.QUESTION_MARK + query, jsonObject);
        KvLogger.instance(this)
                .p("method", " get PMS-Api Push the current user's servers list information")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<PmsOrderContainerInfoResp>>() {
                }.getType()))
                .p("reqBody", jsonObject)
                .p("ReqUrl", pmsApiConfig.getPmsServersPageUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<Page<PmsServersOrderContainerResp>>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }
}
