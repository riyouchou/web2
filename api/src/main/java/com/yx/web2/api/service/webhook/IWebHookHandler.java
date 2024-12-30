package com.yx.web2.api.service.webhook;

import com.stripe.model.StripeObject;
import org.springframework.http.ResponseEntity;

public interface IWebHookHandler {
    ResponseEntity<?> handle(StripeObject stripeObject);
}
