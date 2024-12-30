package com.yx.web2.api.service.payment;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import com.yx.web2.api.common.constant.Web2ApiConstants;
import com.yx.web2.api.common.enums.MonetaryUnit;
import com.yx.web2.api.common.model.StripePaymentMetaData;
import com.yx.web2.api.config.StripeConfig;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.util.StringPool;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StripePaymentService {
    private final StripeConfig stripeConfig;
    private boolean testMode = false;

    @PostConstruct
    public void started() {
        Stripe.apiKey = stripeConfig.getStripeSecretKey();
        Stripe.setConnectTimeout(stripeConfig.getConnectTimeout());
        Stripe.setReadTimeout(stripeConfig.getReadTimeout());
        Stripe.setMaxNetworkRetries(stripeConfig.getMaxNetworkRetries());

        if (Stripe.apiKey.startsWith("sk_test")) {
            testMode = true;
        }
    }

    /**
     * 创建客户
     *
     * @param customerName  客户名称
     * @param customerEmail 客户邮箱
     * @return 客户信息
     * @throws StripeException StripeException
     */
    public Customer createCustomer(String customerName, String customerEmail) throws StripeException {
        CustomerCreateParams createParams = CustomerCreateParams.builder()
                .setName(customerName)
                .setEmail(customerEmail)
                .build();
        return Customer.create(createParams);
    }

    /**
     * 获取客户信息
     *
     * @param customerName  客户名称
     * @param customerEmail 客户邮箱
     * @return 客户信息
     * @throws StripeException StripeException
     */
    public Customer getCustomer(String customerName, String customerEmail) throws StripeException {
        CustomerSearchParams searchParams = CustomerSearchParams.builder()
                .setQuery("name:'" + customerName + "' AND email:'" + customerEmail + "'")
                .setLimit(1L)
                .build();
        CustomerSearchResult customerSearchResult = Customer.search(searchParams);
        if (customerSearchResult == null || customerSearchResult.getData() == null || customerSearchResult.getData().isEmpty()) {
            return null;
        } else {
            return customerSearchResult.getData().get(0);
        }
    }

    public List<PaymentMethod> listPaymentMethod(String customerId) throws StripeException {
        Customer customer = Customer.retrieve(customerId);
        PaymentMethodCollection paymentMethodCollection = customer.listPaymentMethods();
        return paymentMethodCollection.getData();
    }

    /**
     * 创建Card付款方式
     *
     * @param cardNumber 卡号
     * @param expMonth   卡过期月份
     * @param expYear    卡过期年份
     * @param cvc        卡Cvc
     * @param customerId 卡所属的客户Id
     * @param metaData   扩展参数
     * @return StripeException
     */
    public PaymentMethod createCardPaymentMethod(String cardNumber, Long expMonth, Long expYear,
                                                 String cvc, String customerId, Map<String, String> metaData) throws StripeException {
        if (testMode) {
            int randomIndex = RandomUtils.nextInt(0, 1);
            PaymentMethod testPayment = PaymentMethod.retrieve(randomIndex == 0 ? "pm_card_visa" : "pm_card_mastercard");
            Customer customer = Customer.retrieve(customerId);
            List<PaymentMethod> existsPaymentMethodList = customer.listPaymentMethods().getData();
            if (existsPaymentMethodList != null && !existsPaymentMethodList.isEmpty()) {
                for (PaymentMethod paymentMethod : existsPaymentMethodList) {
                    paymentMethod.detach();
                }
            }
            testPayment.attach(PaymentMethodAttachParams.builder().setCustomer(customerId).build());
            testPayment.update(PaymentMethodUpdateParams.builder()
                    .putAllMetadata(metaData)
                    .build());
            return testPayment;
        } else {
            PaymentMethodCreateParams params = PaymentMethodCreateParams.builder()
                    .setType(PaymentMethodCreateParams.Type.CARD)
                    .setCard(PaymentMethodCreateParams.CardDetails.builder()
                            .setNumber(cardNumber)
                            .setExpMonth(expMonth)
                            .setExpYear(expYear)
                            .setCvc(cvc)
                            .build()
                    )
//                    .setBillingDetails(PaymentMethodCreateParams.BillingDetails.builder()
//                            .setAddress(PaymentMethodCreateParams.BillingDetails.Address.builder().build())
//                            .setName("")
//                            .setEmail("")
//                            .setPhone("")
//                            .build())
                    .build();
            PaymentMethod paymentMethod = PaymentMethod.create(params);
            paymentMethod.attach(PaymentMethodAttachParams.builder().setCustomer(customerId).build());
            paymentMethod = paymentMethod.update(PaymentMethodUpdateParams.builder()
                    .putAllMetadata(metaData).build());
            return paymentMethod;
        }
    }

    /**
     * 设置客户普通支付默认付款方式
     *
     * @param customerId      客户Id
     * @param paymentMethodId 付款方式Id
     * @throws StripeException StripeException
     */
    public void setCustomerDefaultPaymentMethod(String customerId, String paymentMethodId) throws StripeException {
        Customer resource = Customer.retrieve(customerId);
        CustomerUpdateParams params =
                CustomerUpdateParams.builder()
                        .setInvoiceSettings(
                                CustomerUpdateParams.InvoiceSettings.builder()
                                        .setDefaultPaymentMethod(paymentMethodId)
                                        .build()
                        )
                        .build();
        resource.update(params);
    }

    public SetupIntent createSetupIntent(CreateSetupIntentParams createSetupIntentParams) throws StripeException {
        Map<String, String> metaData = Maps.newHashMap();
        metaData.put("accountId", createSetupIntentParams.getAccountId().toString());
        metaData.put("tenantId", createSetupIntentParams.getTenantId().toString());

        SetupIntentCreateParams setupIntentCreateParams =
                SetupIntentCreateParams.builder().addAllPaymentMethodType(createSetupIntentParams.getPaymentMethodTypes())
                        .setCustomer(createSetupIntentParams.getCustomerId())
                        .putAllMetadata(metaData)
                        .build();
        return SetupIntent.create(setupIntentCreateParams);
    }

    public Session createSetupIntentCheckoutSession(CreateSetupIntentCheckoutSessionParams createSetupIntentCheckoutSessionParams) throws StripeException {
        SessionCreateParams sessionCreateParams =
                SessionCreateParams.builder()
                        .setMode(SessionCreateParams.Mode.SETUP)
                        .setCurrency(createSetupIntentCheckoutSessionParams.getCurrency())
                        .setCustomer(createSetupIntentCheckoutSessionParams.getCustomerId())
                        .setSuccessUrl(createSetupIntentCheckoutSessionParams.getSuccessUrl())
                        .putMetadata("accountId", createSetupIntentCheckoutSessionParams.getAccountId().toString())
                        .build();

        return Session.create(sessionCreateParams);
    }

    public CustomerSession createCustomerSession(@NotNull String customerId) throws StripeException {
        CustomerSessionCreateParams csParams = CustomerSessionCreateParams.builder()
                .setCustomer(customerId)
                .setComponents(CustomerSessionCreateParams.Components.builder().build())
                .putExtraParam("components[payment_element][enabled]", true)
                .putExtraParam(
                        "components[payment_element][features][payment_method_redisplay]",
                        "enabled"
                )
                .putExtraParam(
                        "components[payment_element][features][payment_method_save]",
                        "enabled"
                )
                .putExtraParam(
                        "components[payment_element][features][payment_method_save_usage]",
                        "on_session"
                )
                .putExtraParam(
                        "components[payment_element][features][payment_method_remove]",
                        "enabled"
                )
                .build();

        return CustomerSession.create(csParams);
    }

    public PaymentIntent createPaymentIntent(@NotNull CreatePaymentIntentParams createPaymentIntentParams) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(createPaymentIntentParams.getHpPrice().multiply(new BigDecimal(100)).longValue())
                .setCurrency(MonetaryUnit.USD.getValue())
                .setCustomer(createPaymentIntentParams.getCustomerId())
                .setPaymentMethod(createPaymentIntentParams.getPaymentMethodId())
//                .addAllPaymentMethodType(Lists.newArrayList("card", "us_bank_account"))
//                .setAutomaticPaymentMethods(
//                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
//                                .setEnabled(true)
//                                .build()
//                )
                .putMetadata("metadata", JSON.toJSONString(createPaymentIntentParams.getMetaData()))
                .build();

        return PaymentIntent.create(params);
    }

    public Session createPaymentCheckoutSession(CreatePaymentCheckoutSessionParams createPaymentCheckoutSessionParams) throws StripeException {
        Map<String, String> metaData = new HashMap<>();
        metaData.put("orderId", createPaymentCheckoutSessionParams.getOrderId());
        metaData.put("paymentOrderId", createPaymentCheckoutSessionParams.getPaymentOrderId());
        SessionCreateParams.PaymentIntentData paymentIntentData = SessionCreateParams.PaymentIntentData.builder()
                .putAllMetadata(metaData)
                .build();
        // create order session
        SessionCreateParams params =
                SessionCreateParams.builder()
                        .setCustomer(createPaymentCheckoutSessionParams.getCustomerId())
                        .setPaymentIntentData(paymentIntentData)
                        .setMode(SessionCreateParams.Mode.PAYMENT)
                        .setSuccessUrl(createPaymentCheckoutSessionParams.getSuccessUrl())
                        .setCancelUrl(createPaymentCheckoutSessionParams.getCancelUrl())
                        .setSavedPaymentMethodOptions(
                                SessionCreateParams.SavedPaymentMethodOptions.builder()
                                        .setPaymentMethodSave(
                                                SessionCreateParams.SavedPaymentMethodOptions.PaymentMethodSave.ENABLED
                                        )
                                        .build()
                        )
                        .addLineItem(SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                        .setCurrency(MonetaryUnit.USD.getValue())
                                        .setUnitAmount(createPaymentCheckoutSessionParams.getHpPrice().multiply(new BigDecimal(100)).longValue())
                                        .setProductData(
                                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                        .setName("Pre-Payment")
                                                        //.setDescription("Region & Spec & Count")
                                                        .addImage("https://d1wqzb5bdbcre6.cloudfront.net/01fe45c5ebf9aff27e2f49317052703d81f437f1a49d56d4625314b2947fc4fd/68747470733a2f2f692e696d6775722e636f6d2f45487952326e502e706e67")
                                                        .build())
                                        .build())
                                .build())
                        .build();

        return Session.create(params);
    }

    public SubscriptionSchedule createSubscriptionSchedule(CreateSubscriptionScheduleParams createSubscriptionScheduleParams) throws StripeException {
        List<SubscriptionScheduleCreateParams.Phase> phaseList = new ArrayList<>();
        for (CreateSubscriptionScheduleParams.Phase phase : createSubscriptionScheduleParams.getPhaseList()) {
            phaseList.add(SubscriptionScheduleCreateParams.Phase.builder()
                    .addItem(SubscriptionScheduleCreateParams.Phase.Item.builder()
                            .setPriceData(SubscriptionScheduleCreateParams.Phase.Item.PriceData.builder()
                                    .setCurrency(Web2ApiConstants.ORDER_ID_MAIN_SOURCE_TYPE)
                                    .setProduct(stripeConfig.getSubscriptionProduct())
                                    .setRecurring(SubscriptionScheduleCreateParams.Phase.Item.PriceData.Recurring.builder()
                                            .setInterval(SubscriptionScheduleCreateParams.Phase.Item.PriceData.Recurring.Interval.MONTH)
                                            .build()
                                    )
                                    .setUnitAmount(phase.getAmount())
                                    .build()
                            )
                            .setQuantity(1L)
                            .build()
                    )
                    .setIterations(phase.getIterations())
                    .build());
        }
        SubscriptionScheduleCreateParams params =
                SubscriptionScheduleCreateParams.builder()
                        .setCustomer(createSubscriptionScheduleParams.getCustomerId())
//                        .setStartDate(SubscriptionScheduleCreateParams.StartDate.NOW)
                        .setMetadata(createSubscriptionScheduleParams.getMetaData())
                        .setStartDate(createSubscriptionScheduleParams.getStartDate())
                        .setEndBehavior(SubscriptionScheduleCreateParams.EndBehavior.CANCEL)
                        .addAllPhase(phaseList)
                        .build();
        return SubscriptionSchedule.create(params);
    }

    public void cancelSubscriptionSchedule(String scheduleId) throws StripeException {
        SubscriptionSchedule subscriptionSchedule = SubscriptionSchedule.retrieve(scheduleId);
        subscriptionSchedule.cancel();
    }

    public void cancelSubscription(String subscriptionId) throws StripeException {
        Subscription subscription = Subscription.retrieve(subscriptionId);
        subscription.cancel();
    }

    @Getter
    @Builder
    public static class CreateSetupIntentParams {
        private String customerId;
        private Long tenantId;
        private Long accountId;
        private List<String> paymentMethodTypes;
    }

    @Getter
    @Builder
    public static class CreateSetupIntentCheckoutSessionParams {
        private String currency;
        private String customerId;
        private String successUrl;
        private Long accountId;
    }

    @Getter
    @Builder
    public static class CreatePaymentIntentParams {
        private StripePaymentMetaData metaData;
        private String customerId;
        private BigDecimal hpPrice;
        private String paymentMethodId;
    }

    @Getter
    @Builder
    public static class CreatePaymentCheckoutSessionParams {
        private String orderId;
        private String paymentOrderId;
        private String customerId;
        private String successUrl;
        private String cancelUrl;
        private BigDecimal hpPrice;
    }

    @Getter
    @Builder
    public static class CreateSubscriptionScheduleParams {
        /**
         * 客户ID
         */
        private String customerId;
        /**
         * 分期开始时间，单位秒
         */
        private Long startDate;
        /**
         * 分期订购的列表
         */
        private List<Phase> phaseList;
        /**
         * meta信息
         */
        private Map<String, String> metaData;

        @Getter
        @Builder
        public static class Phase {

            /**
             * 每期支付金额，单位分
             */
            private Long amount;
            /**
             * 总分期数
             */
            private Long iterations;
        }
    }
}
