package com.yx.pass.remote.pms;

import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Maps;
import com.yx.pass.remote.pms.config.PmsApiConfig;
import com.yx.pass.remote.pms.model.resp.account.TenantAccountResp;
import com.yx.pass.remote.pms.model.resp.account.TenantDetailResp;
import com.yx.pass.remote.pms.model.resp.sepc.SpecPriceResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringPool;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class PmsRemoteAccountService extends PmsRemoteService {
    public PmsRemoteAccountService(RestTemplate restTemplate, PmsApiConfig pmsApiConfig) {
        super(restTemplate, pmsApiConfig);
    }

    /**
     * 获取登录账户信息
     *
     * @param did 账户did
     */
    public R<String> getLoginAccountInfo(String did) {
        Map<String, String> postBody = Maps.newHashMap();
        postBody.put("did", did);
        String query = URLUtil.buildQuery(postBody, StandardCharsets.UTF_8);
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsApiGetLoginAccountUrl() + StringPool.QUESTION_MARK + query);
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<String>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }

    /**
     * 获取Owner账户信息
     *
     * @param tid 账户ID
     */
    public R<TenantAccountResp> getOwnerAccountInfo(Long tid) {
        Map<String, Object> postBody = Maps.newHashMap();
        postBody.put("tid", tid);
        String query = URLUtil.buildQuery(postBody, StandardCharsets.UTF_8);
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsApiGetAccountUrl() + StringPool.QUESTION_MARK + query);
        if (responseEntity != null) {
            R<String> response = JSON.parseObject(responseEntity.getBody(), new TypeReference<R<String>>() {});

            TenantAccountResp tenantAccountResp = JSON.parseObject(response.getData(), TenantAccountResp.class);

            return R.ok(tenantAccountResp);
        }
        return R.failed(143700, "remote call pms failed");
    }

    /**
     * 获取账户信息
     *
     * @param
     */
    public R<String> webPage(Integer current, Integer size, Long uid, Long tid, Long accountId, String accountName,Integer source) {
        Map<String, Object> postBody = Maps.newHashMap();
        postBody.put("current", current);
        postBody.put("size", size);
        postBody.put("uid", uid);
        postBody.put("tid", tid);
        postBody.put("accountId", accountId);
        postBody.put("accountName", accountName);
        postBody.put("source", source);
        String query = URLUtil.buildQuery(postBody, StandardCharsets.UTF_8);
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getWebPageUrl() + StringPool.QUESTION_MARK + query);
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<String>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }
}
