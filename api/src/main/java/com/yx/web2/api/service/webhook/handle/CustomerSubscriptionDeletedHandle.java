package com.yx.web2.api.service.webhook.handle;

import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionSchedule;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.OrderStatus;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.service.IOrderService;
import com.yx.web2.api.service.webhook.WebHookEventType;
import com.yx.web2.api.service.webhook.IWebHookHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.StringUtil;

import java.sql.Timestamp;


@Service(WebHookEventType.CUSTOMER_SUBSCRIPTION_DELETED)
@RequiredArgsConstructor
public class CustomerSubscriptionDeletedHandle implements IWebHookHandler {
    private final IOrderService orderService;

    @Override
    @Master
    public ResponseEntity<?> handle(StripeObject stripeObject) {
        Subscription subscriptionDeleted = (Subscription) stripeObject;
        String scheduleId = subscriptionDeleted.getSchedule();
        KvLogger kvLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.HANDLE_WEB_HOOK_EVENT)
                .p(LogFieldConstants.ACTION, WebHookEventType.CUSTOMER_SUBSCRIPTION_DELETED)
                .p("ScheduleId", scheduleId);
        if (StringUtil.isBlank(scheduleId)) {
            orderService.update(null, Wrappers.lambdaUpdate(OrderEntity.class)
                    .set(OrderEntity::getOrderStatus, OrderStatus.Cancel.getValue())
                    .set(OrderEntity::getFailureReason, "customerSubscriptionDeletedHandle")
                    .set(OrderEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                    .eq(OrderEntity::getSubscriptionId, subscriptionDeleted.getId()));
            kvLogger.i();
        } else {
            try {
                SubscriptionSchedule subscriptionSchedule = SubscriptionSchedule.retrieve(scheduleId);
                orderService.update(null, Wrappers.lambdaUpdate(OrderEntity.class)
                        .set(OrderEntity::getOrderStatus, OrderStatus.Cancel.getValue())
                        .set(OrderEntity::getFailureReason, "customerSubscriptionDeletedHandle")
                        .set(OrderEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                        .eq(OrderEntity::getOrderId, subscriptionSchedule.getMetadata().get("orderId")));
                kvLogger.i();
            } catch (Exception e) {
                kvLogger.p(LogFieldConstants.ERR_MSG, e.getMessage())
                        .e(e);
            }
        }
        return ResponseEntity.ok().build();
    }
}
