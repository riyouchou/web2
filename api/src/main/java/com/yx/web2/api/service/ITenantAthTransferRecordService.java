package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.AthTransferQueryReq;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.order.AthTransferRecordListResp;
import com.yx.web2.api.entity.TenantAthTransferRecordEntity;
import org.yx.lib.utils.util.R;

public interface ITenantAthTransferRecordService extends IService<TenantAthTransferRecordEntity> {
    R<PageResp<AthTransferRecordListResp>> recordList(Long tenantId, AccountModel accountModel, AthTransferQueryReq athTransferQueryReq);
}
