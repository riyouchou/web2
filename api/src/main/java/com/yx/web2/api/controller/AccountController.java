package com.yx.web2.api.controller;


import com.alibaba.fastjson.JSONObject;
import com.yx.pass.remote.pms.model.resp.account.TenantDetailResp;
import com.yx.web2.api.common.constant.RequestAttributeConstants;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.service.IAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringUtil;

import javax.annotation.Resource;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {
    @Resource
    IAccountService accountService;
    /**
     * 获取账号信息
     * @param current
     * @param size
     * @return
     */
    @GetMapping("/webPage")
    R<PageResp<TenantDetailResp>> getAccountPage(Integer current, Integer size, @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,Long accountId,String accountName,Integer source){
        return accountService.webPage(current,size,accountModel,accountId, accountName,source);
    }

}
