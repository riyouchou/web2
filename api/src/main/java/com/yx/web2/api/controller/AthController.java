package com.yx.web2.api.controller;

import com.yx.web2.api.common.constant.RequestAttributeConstants;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.AthTransferQueryReq;
import com.yx.web2.api.common.req.order.OrderQueryReq;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.order.AthTransferRecordListResp;
import com.yx.web2.api.common.resp.order.OrderListResp;
import com.yx.web2.api.service.ITenantAthTransferRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yx.lib.utils.util.R;

@RestController
@RequestMapping("/ath")
@RequiredArgsConstructor
public class AthController {

    private final ITenantAthTransferRecordService athTransferRecordService;

    @PostMapping("/transfer/record")
    public R<PageResp<AthTransferRecordListResp>> transferRecordList(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody AthTransferQueryReq athTransferQueryReq) {
        return athTransferRecordService.recordList(tenantId, accountModel, athTransferQueryReq);
    }
}
