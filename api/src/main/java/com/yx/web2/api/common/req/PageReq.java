package com.yx.web2.api.common.req;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageReq {
    private Long current = 1L;
    private Long size = 20L;
}
