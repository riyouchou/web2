package com.yx.pass.remote.pms.config;

import com.yx.pass.remote.pms.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.yx.lib.utils.rest.RestTemplateConfig;

@Configuration
public class RemotePmsConfig {

    @Bean
    public PmsRemoteRegionService remoteRegionService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            PmsApiConfig pmsApiConfig) {
        return new PmsRemoteRegionService(restTemplate, pmsApiConfig);
    }

    @Bean
    public PmsRemoteSpecService remoteSpecService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            PmsApiConfig pmsApiConfig) {
        return new PmsRemoteSpecService(restTemplate, pmsApiConfig);
    }

    @Bean
    public PmsRemoteAccountService remoteAccountService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            PmsApiConfig pmsApiConfig) {
        return new PmsRemoteAccountService(restTemplate, pmsApiConfig);
    }

    @Bean
    public PmsRemoteContainerOrderService remoteContainerOrderService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            PmsApiConfig pmsApiConfig) {
        return new PmsRemoteContainerOrderService(restTemplate, pmsApiConfig);
    }

    @Bean
    public PmsRemoteTenantService remoteTenantService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            PmsApiConfig pmsApiConfig) {
        return new PmsRemoteTenantService(restTemplate, pmsApiConfig);
    }

    @Bean
    public PmsRemoteOrderService remotePmsOrderService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            PmsApiConfig pmsApiConfig) {
        return new PmsRemoteOrderService(restTemplate, pmsApiConfig);
    }

    @Bean
    public PmsRemoteServersService remoteServersService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            PmsApiConfig pmsApiConfig) {
        return new PmsRemoteServersService(restTemplate, pmsApiConfig);
    }
    @Bean
    public PmsRemoteSshKeyService remoteSshKeyService(
            @Qualifier(RestTemplateConfig.YX_REST_TEMPLATE) RestTemplate restTemplate,
            PmsApiConfig pmsApiConfig) {
        return new PmsRemoteSshKeyService(restTemplate, pmsApiConfig);
    }
}
