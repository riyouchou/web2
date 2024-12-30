package com.yx.pass.remote.pms.model.resp.resource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PmsOrderContainerPageResp implements Serializable {

    private String wholesale;

    private String orderCode;

    private Long effectTime;

    private Long endTime;

    private String effectTimeStr;

    private String endTimeStr;

    private String region;

    private String spec;

    private Long cid;

    private Integer status;

}