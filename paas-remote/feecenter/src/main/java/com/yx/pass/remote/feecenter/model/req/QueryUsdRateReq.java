package com.yx.pass.remote.feecenter.model.req;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Getter
@SuperBuilder
public class QueryUsdRateReq extends FeeCenterReqBase {
    private Date beginDateTime;
    private Date endDateTime;
}
