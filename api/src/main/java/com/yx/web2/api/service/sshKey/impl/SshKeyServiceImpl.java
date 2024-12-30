package com.yx.web2.api.service.sshKey.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.pass.remote.pms.PmsRemoteSshKeyService;
import com.yx.pass.remote.pms.config.PmsApiConfig;
import com.yx.pass.remote.pms.model.req.ServerSshKeyReq;
import com.yx.pass.remote.pms.model.resp.servers.sshkey.ServerSshKeyResp;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.service.sshKey.SshKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.util.R;

@Service
@RequiredArgsConstructor
@Slf4j
public class SshKeyServiceImpl implements SshKeyService {
    private final PmsRemoteSshKeyService pmsRemoteSshKeyService;
    private final PmsApiConfig pmsApiConfig;

    @Override
    public R<Page<ServerSshKeyResp>> page(Integer current, Integer size, AccountModel accountModel) {
        return pmsRemoteSshKeyService.page(current, size, accountModel.getTenantId(), NumberUtils.LONG_ZERO);
    }

    @Override
    public R<Object> add(ServerSshKeyReq req, AccountModel accountModel) {
        return pmsRemoteSshKeyService.handleOperationByUrl(pmsApiConfig.getPmsSshKeyAddUrl(), req, accountModel.getTenantId(), "Add");

    }

    @Override
    public R<Object> delete(ServerSshKeyReq req, AccountModel accountModel) {
        return pmsRemoteSshKeyService.handleOperationByUrl(pmsApiConfig.getPmsSshKeyDeleteUrl(), req, accountModel.getTenantId(), "Delete");
    }

    @Override
    public R<Object> bind(ServerSshKeyReq req, AccountModel accountModel) {
        return pmsRemoteSshKeyService.handleOperationByUrl(pmsApiConfig.getPmsShKeyBindUrl(), req, accountModel.getTenantId(), "Bind");
    }

    @Override
    public R<Object> unbind(ServerSshKeyReq req, AccountModel accountModel) {
        return pmsRemoteSshKeyService.handleOperationByUrl(pmsApiConfig.getPmsShKeyUnBindUrl(), req, accountModel.getTenantId(), "Bind Delete");
    }

    @Override
    public R<Page<ServerSshKeyResp>> availableBind(Integer current, Integer size, Long cid, AccountModel accountModel) {
        return pmsRemoteSshKeyService.page(current, size, accountModel.getTenantId(), cid);
    }
}
