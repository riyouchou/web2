package com.yx.web2.api.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@RefreshScope
@Data
@Component
public class StripeConfig {

    @Value("${stripe.access-key}")
    private String stripeAccessKey;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.webhook-api-key}")
    private String stripeWebHookApiKey;

    @Value("${stripe.connect-timeout:10000}")
    private Integer connectTimeout;

    @Value("${stripe.read-timeout:10000}")
    private Integer readTimeout;

    @Value("${stripe.max-network-retries:3}")
    private Integer maxNetworkRetries;

    @Value("${stripe.subscription-product}")
    private String subscriptionProduct;
}
