package com.yx.pass.remote.pms;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Maps;
import com.yx.pass.remote.pms.config.PmsApiConfig;
import com.yx.pass.remote.pms.model.resp.sepc.ConfigContainerSpecResp;
import com.yx.pass.remote.pms.model.resp.sepc.SpecPriceResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringPool;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pms 区域相关服务
 */
@Slf4j
public class PmsRemoteSpecService extends PmsRemoteService {

    public PmsRemoteSpecService(RestTemplate restTemplate, PmsApiConfig pmsApiConfig) {
        super(restTemplate, pmsApiConfig);
    }

    /**
     * 查询配置规格信息
     *
     * @param specCode 规格code
     * @return 订单信息
     */
    public R<ConfigContainerSpecResp> getSpecInfo(String specCode) {
        Map<String, String> urlParams = Maps.newHashMap();
        urlParams.put("spec", specCode);
        String query = URLUtil.buildQuery(urlParams, StandardCharsets.UTF_8);

        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsApiSpecInfoUrl() + StringPool.QUESTION_MARK + query);
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<ConfigContainerSpecResp>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }

    /**
     * 查询规格列表信息
     *
     * @param osType     操作系统类型
     * @param regionCode 计价区域
     * @author yijian
     * @date 2024/9/19 18:14
     */
    public R<List<SpecPriceResp>> specList(String osType, String regionCode) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("osType", osType);
        jsonObject.put("regionCode", regionCode);
        String query = URLUtil.buildQuery(jsonObject, StandardCharsets.UTF_8);
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsApiSpecListUrl() + StringPool.QUESTION_MARK + query, jsonObject);
        KvLogger.instance(this)
                .p("method", "PMS push for rental specifications and shopping information")
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<List<SpecPriceResp>>>() {
                }.getType()))
                .p("reqBody", query)
                .p("ReqUrl", pmsApiConfig.getPmsApiSpecListUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<List<SpecPriceResp>>>() {
            }.getType());
        }
        return null;

    }

    public R<List<ConfigContainerSpecResp>> specListByOsType(String platformType) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("platformType", platformType);
        ResponseEntity<String> responseEntity = postRoute(pmsApiConfig.getPmsApiSpecList(), jsonObject);
        if (responseEntity != null) {
            String body = responseEntity.getBody();
            log.info("调用pms接口返回结果：{}", body);
            return JSON.parseObject(body, new TypeReference<R<List<ConfigContainerSpecResp>>>() {
            }.getType());
        }
        return null;
    }

    public R<?> addAppDeploy(Long appId, String regionCode, String orderCode, Integer targetVersion, Integer containerTargetCount, Long tid) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("orderCode", orderCode);
        jsonObject.put("appId", appId);
        jsonObject.put("tid", tid);
        jsonObject.put("targetVersion", targetVersion);
        jsonObject.put("regionCode", regionCode);
        jsonObject.put("containerTargetCount", containerTargetCount);
        ResponseEntity<String> responseEntity = postRoute(pmsApiConfig.getPmsAddAppDeploy(), jsonObject);
        if (responseEntity != null) {
            String body = responseEntity.getBody();
            log.info("调用pms接口返回结果：{}", body);
            if (body.contains("ok")) {
                return R.ok(true);
            } else {
                return R.failed(JSONObject.parseObject(body).getString("msg"));
            }
        }
        return null;
    }

    public R<?> pageAppDeploy(Integer current, Integer size, Long appId, Long tid, String regionCode, Integer enable, String queryStr, String state, HttpServletRequest request, Integer appVersion, String listType) {
        String appIdReport = request.getHeader("appId");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("current", current);
        jsonObject.put("size", size);
        jsonObject.put("appId", appId);
        jsonObject.put("tid", tid);
        jsonObject.put("regionCode", regionCode);
        jsonObject.put("enable", enable);
        jsonObject.put("query", queryStr);
        jsonObject.put("state", state);
        jsonObject.put("appIdReport", appIdReport);
        jsonObject.put("appVersion", appVersion);
        jsonObject.put("listType", listType);
        String query = URLUtil.buildQuery(jsonObject, StandardCharsets.UTF_8);

        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsPageAppDeploy() + StringPool.QUESTION_MARK + query, jsonObject);
        if (responseEntity != null) {
            return R.ok(JSONObject.parseObject(responseEntity.getBody()));
        }
        return R.failed();
    }

    public R<?> getCidsBySpecAndRegion(JSONObject cidsByDeviceInfoReq) {
        try {
            ResponseEntity<String> responseEntity = postRoute(pmsApiConfig.getPmsServersCidsByRegionAndSpecUrl(), cidsByDeviceInfoReq);
            if (responseEntity == null || responseEntity.getBody() == null) {
                log.warn("getCidsBySpecAndRegion调用pms接口返回为空");
                return null;
            }
            String body = responseEntity.getBody();
            log.info("getCidsBySpecAndRegion调用pms接口返回结果：{}", body);
            JSONObject jsonObject = JSONObject.parseObject(body);
            if (Objects.equals(jsonObject.getInteger("code"), 0)) {
                JSONArray dataArray = jsonObject.getJSONArray("data");
                if (dataArray != null && !dataArray.isEmpty()) {
                    return R.ok(dataArray);
                } else {
                    return R.failed("Data is empty");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }



        return R.failed("Invalid response");
    }

    public R<?> testUrl(Long id) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        ResponseEntity<String> responseEntity = postRoute(pmsApiConfig.getPmsTestUrl(), jsonObject);
        if (responseEntity != null) {
            String body = responseEntity.getBody();
            log.info("调用pms接口返回结果：{}", body);
            JSONObject jsonObject1 = JSONObject.parseObject(body);
            if (body.contains("ok")) {
                return R.ok(jsonObject1.getString("data"));
            }
        }
        return null;
    }
}
