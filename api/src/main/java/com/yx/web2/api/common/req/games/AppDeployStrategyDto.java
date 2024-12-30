package com.yx.web2.api.common.req.games;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppDeployStrategyDto implements Serializable {

    private Long appId;
    private Long tid;

    private String regionCode;

    private Integer targetVersion;

    private Integer containerTargetCount;

    private String fingerprints;

    private String orderCode;


}
