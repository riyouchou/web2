package com.yx.web2.api.service.webhook;

import org.springframework.http.ResponseEntity;

public interface IWebHookCallbackService {
    ResponseEntity<?> doStripeWebhookCallback(String payload, String sign);
}
