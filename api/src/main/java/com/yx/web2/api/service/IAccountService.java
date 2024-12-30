package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.pass.remote.pms.model.resp.account.TenantDetailResp;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.*;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.order.OrderDetailResp;
import com.yx.web2.api.common.resp.order.OrderListResp;
import com.yx.web2.api.common.resp.order.OrderPayResp;
import com.yx.web2.api.entity.OrderEntity;
import org.yx.lib.utils.util.R;

public interface IAccountService  {

    /**
     * 用户列表
     *
     */
    R<PageResp<TenantDetailResp>> webPage(Integer current ,Integer size,AccountModel accountModel,Long accountId,String accountName,Integer source);


}
