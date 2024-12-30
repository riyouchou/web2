package com.yx.pass.remote.pms.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Data
@RefreshScope
@Configuration
public class PmsApiConfig {

    @Value("${pms.api.ak}")
    private String pmsApiAk;

    @Value("${pms.api.sk}")
    private String pmsApiSk;

    @Value("${pms.api.x-token-expire:3600}")
    private Long xTokenExpireS;

    @Value("${pms.api.dashboard-url}")
    private String pmsApiDashboardUrl;

    @Value("${pms.api.regions-url}")
    private String pmsApiRegionsUrl;

    @Value("${pms.api.spec-list-url}")
    private String pmsApiSpecListUrl;

    @Value("${pms.api.order-list-url}")
    private String pmsApiOrderListUrl;

    @Value("${pms.api.region-info-url}")
    private String pmsApiRegionInfoUrl;

    @Value("${pms.api.spec-info-url}")
    private String pmsApiSpecInfoUrl;

    @Value("${pms.api.get-account-url}")
    private String pmsApiGetAccountUrl;

    @Value("${pms.api.get-login-account-url}")
    private String pmsApiGetLoginAccountUrl;

    @Value("${pms.api.tenant-info-url}")
    private String pmsApiGetTenantInfoUrl;

//    @Value("${pms.api.order-add-url}")
//    private String orderCreateUrl;

    @Value("${pms.api.order-pay-url}")
    private String orderPayUrl;

//    @Value("${pms.api.order-close-url}")
//    private String orderCloseUrl;

    /**
     * 可订购资源列表
     * @author yijian
     * @date 2024/9/18 17:36
     */
    @Value("${pms.api.order-resourceSurvey-url}")
    private String orderResourceSurveyUrl;

    @Value("${pms.api.web-page-url}")
    private String webPageUrl;

    @Value("${pms.api.spec-list}")
    private String pmsApiSpecList;

    @Value("${pms.api.game-add-app-deploy}")
    private String pmsAddAppDeploy;

    @Value("${pms.api.game-page-app-deploy}")
    private String pmsPageAppDeploy;

    @Value("${pms.api.game-test-url}")
    private String pmsTestUrl;

    @Value("${pms.api.deploy-container-strategy-url}")
    private String deployContainerStrategyUrl;

    @Value("${pms.api.servers-page-url}")
    private String pmsServersPageUrl;

    @Value("${pms.api.servers-detail-url}")
    private String pmsServersDetailUrl;

    @Value("${pms.api.servers-find-gpuOrRegion-url}")
    private String pmsServersFindGpuOrRegionUrl;

    @Value("${pms.api.servers-resources-url}")
    private String pmsServersResourcesUrl;

    @Value("${pms.api.servers-cards-url}")
    private String pmsApiServersCardsUrl;

    @Value("${pms.api.servers-cids-by-regionAndSpec}")
    private String pmsServersCidsByRegionAndSpecUrl;

    @Value("${pms.api.ssh-key-page-url}")
    private String pmsSshKeyPageUrl;

    @Value("${pms.api.ssh-key-add-url}")
    private String pmsSshKeyAddUrl;

    @Value("${pms.api.ssh-key-delete-url}")
    private String pmsSshKeyDeleteUrl;

    @Value("${pms.api.ssh-key-bind-url}")
    private String pmsShKeyBindUrl;

    @Value("${pms.api.ssh-key-unbind-url}")
    private String pmsShKeyUnBindUrl;
}
