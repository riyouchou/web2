package com.yx.web2.api.common.req.contract;

import com.yx.web2.api.common.req.PageReq;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ContractQueryReq extends PageReq {
    private Long tenantId;
    private List<Integer> status;
    private String startCreateDate;
    private String endCreateDate;
}
