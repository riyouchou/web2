package com.yx.web2.api.service.webhook.handle;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.entity.CustomerPaymentMethodEntity;
import com.yx.web2.api.service.ICustomerPaymentMethodService;
import com.yx.web2.api.service.payment.StripePaymentService;
import com.yx.web2.api.service.webhook.IWebHookHandler;
import com.yx.web2.api.service.webhook.WebHookEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.StringUtil;

import java.util.Map;

@Service(WebHookEventType.SETUP_INTENT_SUCCEEDED)
@RequiredArgsConstructor
public class SetupIntentSucceededHandle implements IWebHookHandler {
    private final ICustomerPaymentMethodService customerPaymentMethodService;
    private final StripePaymentService stripePaymentService;

    @Override
    @Master
    public ResponseEntity<?> handle(StripeObject stripeObject) {
        SetupIntent setupIntent = (SetupIntent) stripeObject;
        String customerId = setupIntent.getCustomer();
        String setupIntentId = setupIntent.getId();
        Map<String, String> metaData = setupIntent.getMetadata();
        String accountId = metaData.get("accountId");
        String tenantId = metaData.get("tenantId");

        KvLogger kvLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.HANDLE_WEB_HOOK_EVENT)
                .p(LogFieldConstants.ACTION, WebHookEventType.SETUP_INTENT_SUCCEEDED)
                .p("SetupIntentId", setupIntentId)
                .p("CustomerId", customerId)
                .p("OwnerAccountId", accountId)
                .p("TenantId", tenantId);

        String paymentMethodId = setupIntent.getPaymentMethod();
        kvLogger.p("paymentMethodId", paymentMethodId);
        kvLogger.i();
        return ResponseEntity.ok().build();

//        PaymentMethod paymentMethod;
//        try {
//            paymentMethod = PaymentMethod.retrieve(paymentMethodId);
//        } catch (Exception ex) {
//            kvLogger.p(LogFieldConstants.ERR_MSG, "getPaymentMethod")
//                    .e(ex);
//            return ResponseEntity.internalServerError().build();
//        }
//        if (StringUtil.isNotBlank(accountId) && StringUtil.isNotBlank(tenantId)) {
//            CustomerPaymentMethodEntity customerPaymentMethodEntity = customerPaymentMethodService.getSimple(customerId);
//            if (customerPaymentMethodEntity == null) {
//                try {
//                    stripePaymentService.setCustomerDefaultPaymentMethod(customerId, paymentMethodId);
//                } catch (StripeException ex) {
//                    kvLogger.p(LogFieldConstants.ERR_MSG, "setCustomerDefaultPaymentMethodError")
//                            .e(ex);
//                    return ResponseEntity.internalServerError().build();
//                }
//                // setCustomerDefaultPaymentMethod有延时，设置成功后再次查询db是否已经有数据
//                customerPaymentMethodEntity = customerPaymentMethodService.getSimple(customerId);
//                if (customerPaymentMethodEntity == null) {
//                    customerPaymentMethodService.save(CustomerPaymentMethodEntity.builder()
//                            .customerId(customerId)
//                            .tenantId(Long.parseLong(tenantId))
//                            .accountId(Long.parseLong(accountId))
//                            .paymentMethodId(paymentMethodId)
//                            .paymentMethodInfo(JSON.toJSONString(paymentMethod))
//                            .build());
//                    kvLogger.p("created", true).i();
//                } else {
//                    kvLogger.p("oldPaymentMethodId", customerPaymentMethodEntity.getPaymentMethodId());
//                    customerPaymentMethodService.update(new LambdaUpdateWrapper<>(CustomerPaymentMethodEntity.class)
//                            .set(CustomerPaymentMethodEntity::getPaymentMethodId, paymentMethodId)
//                            .set(CustomerPaymentMethodEntity::getPaymentMethodInfo, JSON.toJSONString(paymentMethod))
//                            .eq(CustomerPaymentMethodEntity::getId, customerPaymentMethodEntity.getId())
//                            .eq(CustomerPaymentMethodEntity::getPaymentMethodId, customerPaymentMethodEntity.getPaymentMethodId()));
//                    kvLogger.p("updated", true)
//                            .p("newPaymentMethodId", paymentMethodId)
//                            .i();
//                }
//            } else {
//                try {
//                    stripePaymentService.setCustomerDefaultPaymentMethod(customerId, paymentMethodId);
//                } catch (StripeException ex) {
//                    kvLogger.p(LogFieldConstants.ERR_MSG, "setCustomerDefaultPaymentMethodError")
//                            .e(ex);
//                    return ResponseEntity.internalServerError().build();
//                }
//                kvLogger.p("oldPaymentMethodId", paymentMethodId);
//                customerPaymentMethodService.update(new LambdaUpdateWrapper<>(CustomerPaymentMethodEntity.class)
//                        .set(CustomerPaymentMethodEntity::getPaymentMethodId, paymentMethodId)
//                        .set(CustomerPaymentMethodEntity::getPaymentMethodInfo, JSON.toJSONString(paymentMethod))
//                        .eq(CustomerPaymentMethodEntity::getId, customerPaymentMethodEntity.getId())
//                        .eq(CustomerPaymentMethodEntity::getPaymentMethodId, customerPaymentMethodEntity.getPaymentMethodId()));
//                kvLogger.p("updated", true)
//                        .p("newPaymentMethodId", paymentMethodId)
//                        .i();
//            }
//            return ResponseEntity.ok().build();
//        } else {
//            kvLogger.p(LogFieldConstants.ERR_MSG, "metaData not containsKey 'accountId' or 'tenantId' ")
//                    .e();
//            return ResponseEntity.notFound().build();
//        }
    }
}
