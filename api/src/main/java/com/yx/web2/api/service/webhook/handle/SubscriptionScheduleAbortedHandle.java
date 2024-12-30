package com.yx.web2.api.service.webhook.handle;

import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.stripe.model.StripeObject;
import com.stripe.model.SubscriptionSchedule;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.InstalmentPaymentStatus;
import com.yx.web2.api.common.enums.OrderPaymentStatus;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.entity.OrderPaymentEntity;
import com.yx.web2.api.service.IOrderPaymentService;
import com.yx.web2.api.service.IOrderService;
import com.yx.web2.api.service.webhook.WebHookEventType;
import com.yx.web2.api.service.webhook.IWebHookHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service(WebHookEventType.SUBSCRIPTION_SCHEDULE_ABORTED)
@RequiredArgsConstructor
public class SubscriptionScheduleAbortedHandle implements IWebHookHandler {
    private final IOrderService orderService;
    private final IOrderPaymentService orderPaymentService;

    @Master
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResponseEntity<?> handle(StripeObject stripeObject) {
        SubscriptionSchedule abortedSubscriptionSchedule = (SubscriptionSchedule) stripeObject;
        Map<String, String> metaData = abortedSubscriptionSchedule.getMetadata();
        String orderId = metaData.get("orderId");

        KvLogger kvLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.HANDLE_WEB_HOOK_EVENT)
                .p(LogFieldConstants.ACTION, WebHookEventType.SUBSCRIPTION_SCHEDULE_ABORTED)
                .p("SubscriptionId", abortedSubscriptionSchedule.getSubscription())
                .p("OrderId", orderId);

        List<OrderPaymentEntity> notPaymentList = orderPaymentService.getNotPaymentSubscriptionPayments(orderId);
        if (notPaymentList != null && !notPaymentList.isEmpty()) {
            List<Long> notPaymentIds = notPaymentList.stream().map(OrderPaymentEntity::getId).collect(Collectors.toList());
            orderPaymentService.update(Wrappers.lambdaUpdate(OrderPaymentEntity.class)
                    .set(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.FAILED.getValue())
                    .set(OrderPaymentEntity::getFailureReason, WebHookEventType.SUBSCRIPTION_SCHEDULE_ABORTED)
                    .set(OrderPaymentEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                    .in(OrderPaymentEntity::getId, notPaymentIds)
            );
        }
        orderService.update(Wrappers.lambdaUpdate(OrderEntity.class)
                .set(OrderEntity::getPaymentStatus, OrderPaymentStatus.OverduePayment.getValue())
                .set(OrderEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                .eq(OrderEntity::getOrderId, orderId)
        );
        kvLogger.i();
        return ResponseEntity.ok().build();
    }
}
