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
public class DocusignConfig {

    @Value("${docusign.base-url}")
    private String baseUrl;

    @Value("${docusign.oauth-base-path}")
    private String oAuthBasePath;

    @Value("${docusign.integration-key}")
    private String clientId;

    @Value("${docusign.sdk-user-id}")
    private String userId;

    @Value("${docusign.webhook-callback-sk}")
    private String webhookCallbackSk;

//    @Value("${docusign.private-key-location}")
    private String privateKeyLocation = "C:\\data\\docusgin_private_key.key";
}
