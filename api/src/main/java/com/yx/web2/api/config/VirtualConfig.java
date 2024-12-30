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
public class VirtualConfig {

    @Value("${web2.virtualPaymentTime:30}")
    private int virtualPaymentTime;

    @Value("${web2.virtualUrl}")
    private String virtualUrl;

    @Value("${web2.virtualJob:300}")
    private long virtualJob;
}
