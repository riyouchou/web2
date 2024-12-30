package com.yx.web2.api.service.webhook.handle;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;

import java.sql.Timestamp;

@Service(WebHookEventType.INVOICE_PAID)
@RequiredArgsConstructor
public class InvoicePaidHandle implements IWebHookHandler {
    private final IOrderService orderService;
    private final IOrderPaymentService orderPaymentService;


    @Override
    @Master
    public ResponseEntity<?> handle(StripeObject stripeObject) {
        Invoice invoice = (Invoice) stripeObject;
        KvLogger kvLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.HANDLE_WEB_HOOK_EVENT)
                .p(LogFieldConstants.ACTION, WebHookEventType.INVOICE_PAID)
                .p("PayId", invoice.getPaymentIntent())
                .p("SubscriptionId", invoice.getSubscription());

        String subscriptionId = invoice.getSubscription();
        String payId = invoice.getPaymentIntent();
        long eventTime = invoice.getCreated() * 1000;

        OrderEntity orderEntity = orderService.getOrderBySubscriptionId(subscriptionId);
        if (orderEntity == null) {
            kvLogger.p(LogFieldConstants.ERR_MSG, "not found order by subscriptionId")
                    .e();
            return ResponseEntity.notFound().build();
        }
        long payCount = orderPaymentService.count(Wrappers.lambdaQuery(OrderPaymentEntity.class).eq(OrderPaymentEntity::getPayId, payId));
        if (payCount > 0) {
            kvLogger.p(LogFieldConstants.ERR_MSG, "notify repeat")
                    .i();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String planPayTime = DateUtil.format(DateUtil.date(eventTime), DatePattern.SIMPLE_MONTH_PATTERN);
        OrderPaymentEntity orderPaymentEntity = orderPaymentService.getNotPaymentSubscriptionPayments(orderEntity.getOrderId(), planPayTime);
        if (orderPaymentEntity == null) {
            kvLogger.p(LogFieldConstants.ERR_MSG, "not found paymentOrder or payment is done")
                    .p("orderId", orderEntity.getOrderId())
                    .p("planPayTime", planPayTime)
                    .e();
            return ResponseEntity.notFound().build();
        }
        if (orderPaymentEntity.getPaymentStatus().intValue() == InstalmentPaymentStatus.PAID.getValue()) {
            kvLogger.p(LogFieldConstants.ERR_MSG, "paymentOrder is paid")
                    .p("orderId", orderEntity.getOrderId())
                    .p("orderPaymentId", orderPaymentEntity.getPaymentOrderId())
                    .p("totalPaymentMonth", orderPaymentEntity.getInstalmentMonthTotal())
                    .p("curPaymentMonth", orderPaymentEntity.getInstalmentMonth())
                    .p("planPayTime", planPayTime)
                    .i();
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        orderPaymentService.update(Wrappers.lambdaUpdate(OrderPaymentEntity.class)
                .set(OrderPaymentEntity::getPayId, payId)
                .set(OrderPaymentEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                .set(OrderPaymentEntity::getPayFinishTime, new Timestamp(System.currentTimeMillis()))
                .set(OrderPaymentEntity::getPaymentStatus, InstalmentPaymentStatus.PAID.getValue())
                .eq(OrderPaymentEntity::getId, orderPaymentEntity.getId()));

        orderService.update(Wrappers.lambdaUpdate(OrderEntity.class)
                .set(OrderEntity::getPaymentStatus, OrderPaymentStatus.NormalPayment.getValue())
                .eq(OrderEntity::getId, orderEntity.getId()));

        kvLogger.p("orderId", orderEntity.getOrderId())
                .p("orderPaymentId", orderPaymentEntity.getPaymentOrderId())
                .p("totalPaymentMonth", orderPaymentEntity.getInstalmentMonthTotal())
                .p("curPaymentMonth", orderPaymentEntity.getInstalmentMonth())
                .p("planPayTime", planPayTime)
                .i();

        return ResponseEntity.ok().build();
    }
}
