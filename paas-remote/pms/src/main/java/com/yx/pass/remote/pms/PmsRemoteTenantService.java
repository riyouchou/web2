package com.yx.pass.remote.pms;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Maps;
import com.yx.pass.remote.pms.config.PmsApiConfig;
import com.yx.pass.remote.pms.model.resp.tenant.TenantInfoResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringPool;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class PmsRemoteTenantService extends PmsRemoteService {
    public PmsRemoteTenantService(RestTemplate restTemplate, PmsApiConfig pmsApiConfig) {
        super(restTemplate, pmsApiConfig);
    }

    /**
     * 获取租户信息
     *
     * @param tenantId 租户Id
     */
    public R<TenantInfoResp> getTenantInfo(Long tenantId) {
        Map<String, Long> postBody = Maps.newHashMap();
        postBody.put("tid", tenantId);
        String query = URLUtil.buildQuery(postBody, StandardCharsets.UTF_8);
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsApiGetTenantInfoUrl() + StringPool.QUESTION_MARK + query);
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<TenantInfoResp>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }
}
