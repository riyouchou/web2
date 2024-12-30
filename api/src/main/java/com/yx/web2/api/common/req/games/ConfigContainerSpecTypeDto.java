package com.yx.web2.api.common.req.games;

import lombok.Data;

import java.io.Serializable;


@Data
public class ConfigContainerSpecTypeDto implements Serializable {


    private String resourcePool;

    private String platformType;


}
