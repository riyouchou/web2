package com.yx.web2.api.service.webhook;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.config.StripeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebHookCallbackService implements IWebHookCallbackService {

    private final StripeConfig stripeConfig;
    private final Map<String, IWebHookHandler> webHookHandlerMap;

    @Master
    @Override
    public ResponseEntity<?> doStripeWebhookCallback(String payload, String sign) {
        Event event = null;
        // validate sign
        try {
            event = Webhook.constructEvent(payload, sign, stripeConfig.getStripeWebHookApiKey());
        } catch (Exception ex) {
            // Invalid payload
            return new ResponseEntity<>("Invalid payload", HttpStatus.BAD_REQUEST);
        }
        KvLogger kvLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ORDER_EVENT)
                .p(LogFieldConstants.ACTION, event.getType())
                .p("Payload", JSON.toJSONString(event))
                .p("CallbackApiVersion", event.getApiVersion())
                .p("CurrentUsedApiVersion", Stripe.API_VERSION);
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject;
        if (dataObjectDeserializer.getObject().isPresent()) {
            stripeObject = dataObjectDeserializer.getObject().get();
        } else {
            // Deserialization failed, probably due to an API version mismatch.
            // Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
            // instructions on how to handle this case, or return an error here.
            kvLogger.p(LogFieldConstants.ERR_MSG, "Deserialization failed due to an API version mismatch.");
            return new ResponseEntity<>("Deserialization failed", HttpStatus.BAD_REQUEST);
        }

        // see doc https://docs.stripe.com/billing/subscriptions/overview#subscription-events
        IWebHookHandler webHookHandler = webHookHandlerMap.get(event.getType());
        if (webHookHandler != null) {
            kvLogger.i();
            webHookHandler.handle(stripeObject);
        } else {
            kvLogger.p(LogFieldConstants.ERR_MSG, "There is no corresponding event action")
                    .i();
        }
        return ResponseEntity.ok().build();
    }
}
