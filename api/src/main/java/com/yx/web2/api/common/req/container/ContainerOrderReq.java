package com.yx.web2.api.common.req.container;

import com.yx.web2.api.common.req.PageReq;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContainerOrderReq extends PageReq {
    private String regionCode;

    private String spec;
}
