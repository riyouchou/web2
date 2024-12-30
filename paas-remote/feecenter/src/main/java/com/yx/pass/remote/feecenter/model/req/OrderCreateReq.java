package com.yx.pass.remote.feecenter.model.req;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;

/**
 * feeCenter 创建订单请求信息
 */
@Getter
@SuperBuilder
public class OrderCreateReq extends FeeCenterReqBase {
    /**
     * 三方订单编号，如:web2
     */
    private String thirdPartyOrderCode;
    /**
     * 订单类型，0：普通订单，1：授信订单
     */
    private int orderType;
    /**
     * 是否自动续租
     */
    private Boolean autoRenew;
    /**
     * 下单渠道，1:pms，2：web2
     */
    private int businessChannel;
    /**
     * 当前汇率
     */
    private String usdRate;
    /**
     * 订购时长
     */
    private Integer orderDuration;
    /**
     * 订购周期 1:年 2:月 3:天
     */
    private Integer orderPeriod;
    /**
     * 下单资源列表
     */
    private List<Resource> resources;

    @Getter
    @Builder
    public static class Resource {
        /**
         * 资源规格信息
         */
        private String spec;
        /**
         * 资源子规格信息，BM资源下单字段为必传
         */
        private String subSpec;
        /**
         * 资源计价区域
         */
        private String region;
        /**
         * 资源类型：BM、ARS、HE
         */
        private String resourcePool;
        /**
         * 资源部署区域
         */
        private String deployRegion;
        /**
         * 需要订购资源数量
         */
        private Integer count;
    }
}
