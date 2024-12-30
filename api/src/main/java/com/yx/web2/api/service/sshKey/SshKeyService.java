package com.yx.web2.api.service.sshKey;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.pass.remote.pms.model.req.ServerSshKeyReq;
import com.yx.pass.remote.pms.model.resp.resource.PmsServersOrderContainerResp;
import com.yx.pass.remote.pms.model.resp.servers.ServerRegionAndGpuTypeResp;
import com.yx.pass.remote.pms.model.resp.servers.ServerSysInfoResp;
import com.yx.pass.remote.pms.model.resp.servers.ServersContainerResp;
import com.yx.pass.remote.pms.model.resp.servers.page.CustomPage;
import com.yx.pass.remote.pms.model.resp.servers.sshkey.ServerSshKeyResp;
import com.yx.web2.api.common.model.AccountModel;
import org.yx.lib.utils.util.R;

import java.util.List;

public interface SshKeyService {


    R<Page<ServerSshKeyResp>> page(Integer current, Integer size, AccountModel accountModel);

    R<Object> add(ServerSshKeyReq req, AccountModel accountModel);

    R<Object> delete(ServerSshKeyReq req, AccountModel accountModel);

    R<Object> bind(ServerSshKeyReq req, AccountModel accountModel);

    R<Object> unbind(ServerSshKeyReq req, AccountModel accountModel);

    R<Page<ServerSshKeyResp>> availableBind(Integer current, Integer size, Long cid, AccountModel accountModel);
}
