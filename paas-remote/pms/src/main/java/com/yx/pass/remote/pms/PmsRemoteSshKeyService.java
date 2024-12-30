package com.yx.pass.remote.pms;

import cn.hutool.core.util.URLUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.pass.remote.pms.config.PmsApiConfig;
import com.yx.pass.remote.pms.model.req.ServerSshKeyReq;
import com.yx.pass.remote.pms.model.resp.servers.sshkey.ServerSshKeyResp;
import com.yx.pass.remote.pms.model.resp.tenant.TenantInfoResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.SpringContextHolder;
import org.yx.lib.utils.util.StringPool;

import java.nio.charset.StandardCharsets;

/**
 * Pms Servers service
 */
public class PmsRemoteSshKeyService extends PmsRemoteService {

    public PmsRemoteSshKeyService(RestTemplate restTemplate, PmsApiConfig pmsApiConfig) {
        super(restTemplate, pmsApiConfig);
    }

    public R<Page<ServerSshKeyResp>> page(Integer current, Integer size, Long tenantId, Long cid) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("current", current);
        jsonObject.put("size", size);
        jsonObject.put("cid", cid);
        jsonObject.put("tid", tenantId);
        String query = URLUtil.buildQuery(jsonObject, StandardCharsets.UTF_8).replace("%20", " ");
        ResponseEntity<String> responseEntity = getRoute(pmsApiConfig.getPmsSshKeyPageUrl() + StringPool.QUESTION_MARK + query, jsonObject);
        KvLogger.instance(this)
                .p("method", " get PMS-Api Push the current user's sshKey list information")
                .p("resBody", JSONObject.toJSONString(JSON.parseObject(responseEntity.getBody(), new TypeReference<R<Page<ServerSshKeyResp>>>() {
                }.getType())))
                .p("reqBody", jsonObject)
                .p("ReqUrl", pmsApiConfig.getPmsSshKeyPageUrl())
                .i();
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<Page<ServerSshKeyResp>>>() {
            }.getType());
        }
        return R.failed(143700, "remote call pms failed");
    }


    public R<Object> handleOperationByUrl(String reqUrl, ServerSshKeyReq req, Long tenantId, String operation) {
        req.setTid(tenantId);
        PmsRemoteTenantService tenantService = SpringContextHolder.getBean(PmsRemoteTenantService.class);
        R<TenantInfoResp> tenantInfo = tenantService.getTenantInfo(tenantId);
        if (tenantInfo == null || tenantInfo.getCode() != R.ok().getCode()) {
            return R.failed(404, "not found tid info from pms");
        }
        ResponseEntity<String> responseEntity = postRoute(reqUrl, JSONObject.toJSONString(req), tenantInfo.getData().getAkSk().getAk(), tenantInfo.getData().getAkSk().getSk());

        KvLogger.instance(this)
                .p("method", String.format("Get PMS push %s SshKey information", operation))
                .p("resBody", JSON.parseObject(responseEntity.getBody(), new TypeReference<R<Object>>() {
                }.getType()))
                .p("reqBody", JSONObject.toJSONString(req))
                .p("tenantInfo-id", tenantInfo.getData().getId())
                .p("tenantInfo-ak", tenantInfo.getData().getAkSk().getAk())
                .p("tenantInfo-sk", tenantInfo.getData().getAkSk().getSk())
                .p("reqUrl", reqUrl)
                .i();
        if (responseEntity != null) {
            R<Object> r = JSON.parseObject(responseEntity.getBody(), new cn.hutool.core.lang.TypeReference<R<Object>>() {
            }.getType());
            return r;
        }
        return null;
    }
}
