package com.yx.pass.remote.pms.model.req;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class PmsOrderContainerReq {

    private String region;

    private String spec;

    private Long tid;

    private Long current;

    private Long size;

}
