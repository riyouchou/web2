package com.yx.pass.remote.feecenter;

import com.yx.pass.remote.feecenter.config.FeeCenterApiConfig;
import org.springframework.web.client.RestTemplate;

/**
 * FeeCenter 规格/ 谷相关服务
 */
public class FeeCenterRemoteFeeCenterSpecOrderService extends FeeCenterRemoteService {

    public FeeCenterRemoteFeeCenterSpecOrderService(RestTemplate restTemplate, FeeCenterApiConfig feeCenterApiConfig) {
        super(restTemplate, feeCenterApiConfig);
    }
}
