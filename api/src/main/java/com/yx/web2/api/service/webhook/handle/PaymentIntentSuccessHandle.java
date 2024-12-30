package com.yx.web2.api.service.webhook.handle;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.InstalmentPaymentStatus;
import com.yx.web2.api.common.enums.OrderPaymentStatus;
import com.yx.web2.api.common.enums.OrderStatus;
import com.yx.web2.api.common.model.StripePaymentMetaData;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.entity.OrderPaymentEntity;
import com.yx.web2.api.service.IOrderPaymentService;
import com.yx.web2.api.service.IOrderService;
import com.yx.web2.api.service.webhook.IWebHookHandler;
import com.yx.web2.api.service.webhook.WebHookEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringPool;
import org.yx.lib.utils.util.StringUtil;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service(WebHookEventType.PAYMENT_INTENT_SUCCEEDED)
@RequiredArgsConstructor
public class PaymentIntentSuccessHandle implements IWebHookHandler {
    private final IOrderService orderService;
    private final IOrderPaymentService orderPaymentService;

    @Override
    @Master
    @Transactional
    public ResponseEntity<?> handle(StripeObject stripeObject) {
        PaymentIntent paymentIntent = (PaymentIntent) stripeObject;
        // 订阅支付成功不处理，在INVOICE_PAID里面处理订阅逻辑
        if (paymentIntent.getDescription() != null && (paymentIntent.getDescription().equals("Subscription creation") ||
                paymentIntent.getDescription().equals("Subscription update") )) {
            return ResponseEntity.ok().build();
        }
        //获取meta data
        String orderInfoStr = paymentIntent.getMetadata() == null ? "" : paymentIntent.getMetadata().get("metadata");
        KvLogger logger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.HANDLE_WEB_HOOK_EVENT)
                .p(LogFieldConstants.ACTION, WebHookEventType.PAYMENT_INTENT_SUCCEEDED)
                .p("OrderInfoStr", orderInfoStr);
        if (StringUtil.isBlank(orderInfoStr)) {
            logger.p(LogFieldConstants.ERR_MSG, "not found metadata")
                    .e();
            return ResponseEntity.notFound().build();
        }
        logger.i();
        StripePaymentMetaData metaData = JSON.parseObject(orderInfoStr, new TypeReference<StripePaymentMetaData>() {
        }.getType());

        for (StripePaymentMetaData.OrderIdInfo orderIdInfo : metaData.getMetaData()) {
            Long oid = orderIdInfo.getOid();
            List<Long> pIds = orderIdInfo.getPIds();
            KvLogger kvLogger = KvLogger.instance(this)
                    .p(LogFieldConstants.EVENT, Web2LoggerEvents.HANDLE_WEB_HOOK_EVENT)
                    .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.HANDLE_WEB_HOOK_EVENT_ACTION_PROCESS_ORDER)
                    .p("Oid", oid)
                    .p("StripePaymentResult", true)
                    .p("PIds", JSON.toJSONString(pIds));

            OrderEntity orderEntity = orderService.getById(oid);
            if (orderEntity == null) {
                kvLogger.p(LogFieldConstants.ERR_MSG, "not found order")
                        .i();
                return ResponseEntity.notFound().build();
            }
            List<OrderPaymentEntity> orderPaymentEntities = orderPaymentService.listByIds(pIds);
            if (orderPaymentEntities == null || orderPaymentEntities.isEmpty()) {
                kvLogger.p(LogFieldConstants.ERR_MSG, "not found paymentOrder")
                        .i();
                return ResponseEntity.notFound().build();
            }
            if (orderPaymentEntities.stream().anyMatch(item -> item.getPaymentStatus().intValue() == InstalmentPaymentStatus.PAID.getValue())) {
                kvLogger.p(LogFieldConstants.ERR_MSG, "paymentOrder is already paid")
                        .i();
                return ResponseEntity.ok().build();
            }
            // update paymentOrder
            OrderPaymentEntity toUpdateOrderPaymentEntity = OrderPaymentEntity.builder()
                    .payFinishTime(new Timestamp(System.currentTimeMillis()))
                    .lastUpdateTime(new Timestamp(System.currentTimeMillis()))
                    .payId(paymentIntent.getId())
                    .paymentStatus(InstalmentPaymentStatus.PAID.getValue())
                    .build();
            orderPaymentService.update(toUpdateOrderPaymentEntity,
                    Wrappers.lambdaUpdate(OrderPaymentEntity.class).in(OrderPaymentEntity::getId,
                            orderPaymentEntities.stream().map(OrderPaymentEntity::getId).collect(Collectors.toList())));
            // update order
            orderService.update(OrderEntity.builder()
                            .orderStatus(OrderStatus.AwaitingPaymentReceipt.getValue())
                            .paymentStatus(OrderPaymentStatus.NormalPayment.getValue())
                            .lastUpdateTime(new Timestamp(System.currentTimeMillis()))
                            .build(),
                    Wrappers.lambdaUpdate(OrderEntity.class).eq(OrderEntity::getId, orderEntity.getId()));
            kvLogger.p("processResult", true)
                    .p("orderStatus", OrderStatus.AwaitingPaymentReceipt.getName())
                    .p("paymentStatus", OrderPaymentStatus.NormalPayment.getName())
                    .i();
        }
        return ResponseEntity.ok().build();
    }
}
