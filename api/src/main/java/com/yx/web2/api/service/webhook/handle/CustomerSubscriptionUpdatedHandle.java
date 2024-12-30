package com.yx.web2.api.service.webhook.handle;

import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionSchedule;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.service.IOrderService;
import com.yx.web2.api.service.webhook.IWebHookHandler;
import com.yx.web2.api.service.webhook.WebHookEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;

import java.sql.Timestamp;
import java.util.Map;

@Service(WebHookEventType.CUSTOMER_SUBSCRIPTION_UPDATED)
@RequiredArgsConstructor
public class CustomerSubscriptionUpdatedHandle implements IWebHookHandler {
    private final IOrderService orderService;

    @Override
    @Master
    public ResponseEntity<?> handle(StripeObject stripeObject) {
        Subscription subscriptionUpdate = (Subscription) stripeObject;
        KvLogger kvLogger = KvLogger.instance(this);
        try {
            SubscriptionSchedule createdSubscriptionSchedule = SubscriptionSchedule.retrieve(subscriptionUpdate.getSchedule());
            Map<String, String> metaData = createdSubscriptionSchedule.getMetadata();

            kvLogger.p(LogFieldConstants.EVENT, Web2LoggerEvents.HANDLE_WEB_HOOK_EVENT)
                    .p(LogFieldConstants.ACTION, WebHookEventType.CUSTOMER_SUBSCRIPTION_CREATED)
                    .p("SubscriptionId", subscriptionUpdate.getId())
                    .p("SubscriptionScheduleId", createdSubscriptionSchedule.getId())
                    .p("OrderId", metaData.get("orderId"));

            orderService.update(null, Wrappers.lambdaUpdate(OrderEntity.class)
                    .set(OrderEntity::getSubscriptionId, subscriptionUpdate.getId())
                    .set(OrderEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                    .eq(OrderEntity::getOrderId, metaData.get("orderId")));
            kvLogger.i();
        } catch (Exception ex) {
            kvLogger.p(LogFieldConstants.ERR_MSG, ex.getMessage())
                    .e(ex);
        }
        return ResponseEntity.ok().build();
    }
}
