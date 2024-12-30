package com.yx.pass.remote.feecenter.config;

import com.yx.pass.remote.feecenter.FeeCenterRemoteFeeCenterSpecOrderService;
import com.yx.pass.remote.feecenter.FeeCenterRemoteOrderService;
import com.yx.pass.remote.feecenter.FeeCenterRemotePriceService;
import com.yx.pass.remote.feecenter.FeeCenterRemoteWalletService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.rest.RestTemplateConfig;

@Configuration
public class RemoteFeeCenterConfig {

    @Bean
    public FeeCenterRemoteOrderService remoteFreeCenterOrderService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            FeeCenterApiConfig feeCenterApiConfig) {
        return new FeeCenterRemoteOrderService(restTemplate, feeCenterApiConfig);
    }

    @Bean
    public FeeCenterRemotePriceService remoteFreeCenterPriceService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            FeeCenterApiConfig feeCenterApiConfig) {
        return new FeeCenterRemotePriceService(restTemplate, feeCenterApiConfig);
    }

    @Bean
    public FeeCenterRemoteWalletService remoteFreeCenterWalletService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            FeeCenterApiConfig feeCenterApiConfig) {
        return new FeeCenterRemoteWalletService(restTemplate, feeCenterApiConfig);
    }

    @Bean
    public FeeCenterRemoteFeeCenterSpecOrderService remoteFeeCenterSpecOrderService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            FeeCenterApiConfig feeCenterApiConfig) {
        return new FeeCenterRemoteFeeCenterSpecOrderService(restTemplate, feeCenterApiConfig);
    }
}
