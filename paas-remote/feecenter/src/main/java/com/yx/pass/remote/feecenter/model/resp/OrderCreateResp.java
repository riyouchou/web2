package com.yx.pass.remote.feecenter.model.resp;

import com.yx.pass.remote.feecenter.model.req.OrderCreateReq;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderCreateResp {
    private String orderCode;
    private Integer orderStatus;
    private Integer orderType;
    private Boolean autoRew;
    private String tid;
    private String resourceType;
    private String athTotal;
    private String dailyAth;
    private String discountTotal;
    private Long effectiveTime;
    private Long endTime;
    private List<OrderCreateReq.Resource> resources;

    @Getter
    @Setter
    public static class Resource {
        private String spec;
        private String subSpec;
        private String region;
        private String resourcePool;
        private String deployRegion;
        private Long count;
    }
}
