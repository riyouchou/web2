package com.yx.web2.api.common.req.order;

import com.yx.web2.api.common.req.PageReq;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AthTransferQueryReq extends PageReq {
    private String orderId;
    private String accountName;
}
