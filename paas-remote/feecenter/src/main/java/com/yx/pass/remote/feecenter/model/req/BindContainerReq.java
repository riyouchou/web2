package com.yx.pass.remote.feecenter.model.req;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@SuperBuilder
public class BindContainerReq {
    private Long tid;
    private String orderCode;
    private List<Long> cids;
}
