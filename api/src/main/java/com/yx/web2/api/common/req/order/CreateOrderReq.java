package com.yx.web2.api.common.req.order;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.MD5Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderReq {
    private Integer serviceDuration;
    private Integer serviceDurationPeriod;
    private Boolean autoRenew = false;
    private String initialPrice;
    private List<OrderDevice> devices;
    private String redirectUrl;
    // 1:ARS, 2:BM
    private Integer orderResourcePool = 1;


    @Getter
    @Setter
    public static class OrderDevice {
        private String regionCode;
        private String spec;
        private String subSpec;
        private Integer quantity;
        private String resourcePool;
        private String unitPrice;
        private String deviceInfo;
        private String deployRegionCode;
    }

    public String toMd5() {
        return MD5Utils.md5Hex(JSON.toJSONString(this), "UTF-8");
    }
}
