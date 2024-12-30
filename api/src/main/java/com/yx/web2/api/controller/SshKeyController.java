package com.yx.web2.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yx.pass.remote.pms.model.req.ServerSshKeyReq;
import com.yx.pass.remote.pms.model.resp.servers.sshkey.ServerSshKeyResp;
import com.yx.web2.api.common.constant.RequestAttributeConstants;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.service.sshKey.SshKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;
import org.yx.lib.utils.util.R;

import javax.annotation.Resource;

/**
 * packageName com.yx.web2.api.controller
 * Dashboard 缺省页显示
 *
 * @author YI-JIAN-ZHANG
 * @className DashboardController
 * @date 2024/9/4
 */
@RefreshScope
@RestController
@RequestMapping("/sshKey")
@RequiredArgsConstructor
public class SshKeyController {
    @Resource
    private SshKeyService sshKeyService;

    @GetMapping("/page")
    public R<Page<ServerSshKeyResp>> page(@RequestParam(defaultValue = "1") Integer current,
                                          @RequestParam(defaultValue = "10") Integer size, @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return sshKeyService.page(current, size, accountModel);
    }

    @PostMapping("/add")
    public R<Object> add(@RequestBody ServerSshKeyReq req, @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return sshKeyService.add(req, accountModel);
    }

    @PostMapping("/delete")
    public R<Object> detele(@RequestBody ServerSshKeyReq req, @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return sshKeyService.delete(req, accountModel);
    }

    @PostMapping("/bind")
    public R<Object> bind(@RequestBody ServerSshKeyReq req, @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return sshKeyService.bind(req, accountModel);
    }

    @PostMapping("/unbind")
    public R<Object> unbind(@RequestBody ServerSshKeyReq req, @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        req.setId(req.getSshKeyRelId());
        return sshKeyService.unbind(req, accountModel);
    }

    @GetMapping("/available/bind")
    public R<Page<ServerSshKeyResp>> availableBind(@RequestParam(defaultValue = "1") Integer current,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam Long cid,
                                   @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return sshKeyService.availableBind(current, size, cid, accountModel);
    }

}
