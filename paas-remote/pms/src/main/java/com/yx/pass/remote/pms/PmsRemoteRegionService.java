package com.yx.pass.remote.pms;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Maps;
import com.yx.pass.remote.pms.config.PmsApiConfig;
import com.yx.pass.remote.pms.model.resp.DashboardRegionServerResp;
import com.yx.pass.remote.pms.model.resp.region.RegionInfoResp;
import com.yx.pass.remote.pms.model.resp.servers.ServerSysInfoResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringPool;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Pms 区域相关服务
 */
public class PmsRemoteRegionService extends PmsRemoteService {

    public PmsRemoteRegionService(RestTemplate restTemplate, PmsApiConfig pmsApiConfig) {
        super(restTemplate, pmsApiConfig);
    }


    /**
     * 查询含有容器的计价区域列表
     *
     * @param object 区域code
     */
    public R<List<RegionInfoResp>> getRegionList(JSONObject object) {
        String query = URLUtil.buildQuery(object, StandardCharsets.UTF_8);
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsApiRegionsUrl() + StringPool.QUESTION_MARK + query, object);
        KvLogger.instance(this)
                .p("method", "get PMS push  region information")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<List<RegionInfoResp>>>() {
                }.getType()))
                .p("reqBody", query)
                .p("ReqUrl", pmsApiConfig.getPmsApiRegionsUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<List<RegionInfoResp>>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }

    /**
     * 查询区域信息
     *
     * @param regionCode 区域code
     */
    public R<RegionInfoResp> getRegionInfo(String regionCode) {
        return getRegionInfo(regionCode, "");
    }

    /**
     * 查询区域信息
     *
     * @param regionCode   区域code
     * @param resourcePool 资源池类型
     */
    public R<RegionInfoResp> getRegionInfo(String regionCode, String resourcePool) {
        Map<String, String> postBody = Maps.newHashMap();
        postBody.put("regionCode", regionCode);
        postBody.put("resourcePool", resourcePool);
        String query = URLUtil.buildQuery(postBody, StandardCharsets.UTF_8);
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsApiRegionInfoUrl() + StringPool.QUESTION_MARK + query);
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<RegionInfoResp>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }

    /**
     * 容器列表 缺省页显示
     * @author yijian
     * @date 2024/9/19 17:58
     */
    public R<DashboardRegionServerResp> dashboard(String resourcePool, Long tenantId, String scene) {
        JSONObject params = new JSONObject();
        params.put("resourcePool", resourcePool);
        params.put("wholeSaleTid", tenantId);
        params.put("scene", scene);
        ResponseEntity<String> responseEntity = postRoute(pmsApiConfig.getPmsApiDashboardUrl(), params);
        KvLogger.instance(this)
                .p("method", " Get PMS-Api Push DashBoard Info")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<DashboardRegionServerResp>>() {
                }.getType()))
                .p("reqBody", params)
                .p("ReqUrl", pmsApiConfig.getPmsApiDashboardUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<DashboardRegionServerResp>>() {
            }.getType());
        }
        return null;
    }
}
