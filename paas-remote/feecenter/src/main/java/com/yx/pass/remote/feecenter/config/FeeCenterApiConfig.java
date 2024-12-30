package com.yx.pass.remote.feecenter.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Data
@RefreshScope
@Configuration
public class FeeCenterApiConfig {

    @Value("${fee-center.api.ak}")
    private String ak;

    @Value("${fee-center.api.sk}")
    private String sk;

    @Value("${fee-center.api.x-token-expireS:3600}")
    private Long xTokenExpireS;

    @Value("${fee-center.api.order-create-url}")
    private String orderCreateUrl;

    @Value("${fee-center.api.order-pay-url}")
    private String orderPayUrl;

    @Value("${fee-center.api.order-close-url}")
    private String orderCloseUrl;

    @Value("${fee-center.api.wallet-transfer-url}")
    private String walletTransferUrl;

    @Value("${fee-center.api.price-usdrate-url}")
    private String priceUsdRateUrl;

    @Value("${fee-center.api.order-exists-url}")
    private String orderExistsUrl;

    @Value("${fee-center.api.order-bind-url}")
    private String orderBindUrl;

    @Value("${fee-center.api.wallet-transfer-back-admin-url}")
    private String walletTransferToAdminUrl;
}
