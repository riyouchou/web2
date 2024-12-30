package com.yx.pass.remote.feecenter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Maps;
import com.yx.pass.remote.feecenter.config.FeeCenterApiConfig;
import com.yx.pass.remote.feecenter.model.req.WalletTransferToAdminReq;
import com.yx.pass.remote.feecenter.model.req.WalletTransferToTenantReq;
import com.yx.pass.remote.feecenter.model.resp.WalletTransferResp;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.util.R;

import java.util.Map;

/**
 * FeeCenter 钱包相关服务
 * 1：钱包转账
 */
public class FeeCenterRemoteWalletService extends FeeCenterRemoteService {

    public FeeCenterRemoteWalletService(RestTemplate restTemplate, FeeCenterApiConfig feeCenterApiConfig) {
        super(restTemplate, feeCenterApiConfig);
    }

    /**
     * 租户钱包转账
     *
     * @param walletTransferToTenantReq 钱包转账请求
     * @return 钱包转账结果
     */
    public R<WalletTransferResp> transferToTenant(Long adminTid, WalletTransferToTenantReq walletTransferToTenantReq) {
        Map<String, String> headers = Maps.newHashMap();
        headers.put(H_X_USER, buildXUser(adminTid, walletTransferToTenantReq.getTenantType(), walletTransferToTenantReq.getAccountId()));
        ResponseEntity<String> responseEntity = postRoute(feeCenterApiConfig.getWalletTransferUrl(), walletTransferToTenantReq, headers);
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<WalletTransferResp>>() {
            }.getType());
        }
        return R.failed(143600, "remote call fee-center failed");
    }

    /**
     * Admin钱包转账
     *
     * @param walletTransferToAdminReq 钱包转账请求
     * @return 钱包转账结果
     */
    public R<WalletTransferResp> transferToAdmin(Long adminTid, WalletTransferToAdminReq walletTransferToAdminReq) {
        Map<String, String> headers = Maps.newHashMap();
        headers.put(H_X_USER, buildXUser(adminTid, walletTransferToAdminReq.getTenantType(), walletTransferToAdminReq.getAccountId()));
        ResponseEntity<String> responseEntity = postRoute(feeCenterApiConfig.getWalletTransferToAdminUrl(), walletTransferToAdminReq, headers);
        if (responseEntity != null) {
            return JSON.parseObject(responseEntity.getBody(), new TypeReference<R<WalletTransferResp>>() {
            }.getType());
        }
        return R.failed(143600, "remote call fee-center failed");
    }
}
