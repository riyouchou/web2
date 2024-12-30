package com.yx.web2.api.service;

import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.BillQueryReq;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.order.BillListResp;
import com.yx.web2.api.common.resp.order.OrderInvoiceResp;
import com.yx.web2.api.common.resp.order.OrderReceiptResp;
import org.yx.lib.utils.util.R;

public interface IBillService {
    R<PageResp<BillListResp>> list(Long tenantId, AccountModel accountModel, BillQueryReq billQueryReq);

    OrderInvoiceResp invoice(BillQueryReq billQueryReq);

    OrderReceiptResp receipt(BillQueryReq billQueryReq);
}
