package com.yx.pass.remote.pms.model.resp.sepc;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class OrderResourceSurveyResp implements Serializable {
    private String region;

    private String spec;

    private String specName;

    private String regionName;

    private Integer count;
}
