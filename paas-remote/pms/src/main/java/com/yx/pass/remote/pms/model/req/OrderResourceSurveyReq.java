package com.yx.pass.remote.pms.model.req;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class OrderResourceSurveyReq implements Serializable {
    private String region;

    private String spec;

    private String resourcePool;

    private Long current = 1L;

    private Long size = 20L;
}
