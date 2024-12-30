package com.yx.web2.api.common.req.container.dto;

import com.yx.web2.api.common.req.PageReq;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class FeeApiDTO  extends PageReq implements Serializable {
    
    private String region;

    private String spec;

    private String resourcePool;
}
