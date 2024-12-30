package com.yx.web2.api.controller;

import com.yx.web2.api.common.constant.RequestAttributeConstants;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.BillQueryReq;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.order.BillListResp;
import com.yx.web2.api.common.resp.order.OrderDetailResp;
import com.yx.web2.api.common.resp.order.OrderInvoiceResp;
import com.yx.web2.api.common.resp.order.OrderReceiptResp;
import com.yx.web2.api.service.IBillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yx.lib.utils.util.R;

@RestController
@RequestMapping("/bill")
@RequiredArgsConstructor
public class BillController {

    private final IBillService billService;

    /**
     * 查询账单列表
     *
     * @param tenantId     租户ID
     * @param accountModel 账户信息
     * @param billQueryReq 查询条件
     * @return R
     */
    @PostMapping("/list")
    public R<PageResp<BillListResp>> billList(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody BillQueryReq billQueryReq) {
        return billService.list(tenantId, accountModel, billQueryReq);
    }

    /**
     * 查询账单列表
     *
     * @param billQueryReq 查询条件
     * @return R
     */
    @PostMapping("/invoice")
    public R<OrderInvoiceResp> invoiceDownload(
            @RequestBody BillQueryReq billQueryReq) {
        return R.ok(billService.invoice(billQueryReq));
    }

    /**
     * 查询账单列表
     *
     * @param billQueryReq 查询条件
     * @return R
     */
    @PostMapping("/receipt")
    public R<OrderReceiptResp> receiptDowndload(
            @RequestBody BillQueryReq billQueryReq) {
        return R.ok(billService.receipt(billQueryReq));
    }
}
