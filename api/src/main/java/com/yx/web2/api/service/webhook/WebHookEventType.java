package com.yx.web2.api.service.webhook;

public interface WebHookEventType {
    /**
     * Sent when a PaymentIntent has canceled payment.
     */
    String PAYMENT_INTENT_CANCELED = "payment_intent.canceled";
    /**
     * Sent when a PaymentIntent has failed payment.
     */
    String PAYMENT_INTENT_FAILED = "payment_intent.payment_failed";
    /**
     * Sent when a PaymentIntent has successfully completed payment.
     */
    String PAYMENT_INTENT_SUCCEEDED = "payment_intent.succeeded";
    /**
     * Sent when the invoice requires customer authentication
     */
    String INVOICE_PAYMENT_ACTION_REQUIRED = "invoice.payment_action_required";

    /**
     * Occurs whenever an invoice payment attempt succeeds or an invoice is marked as paid out-of-ban
     */
    String INVOICE_PAID = "invoice.paid";
    /**
     * A payment for an invoice failed. The PaymentIntent status changes to requires_action. The status of the subscription continues to be incomplete only for the subscription’s first invoice
     */
    String INVOICE_PAYMENT_FAILED = "invoice.payment_failed";
    /**
     * Sent when a subscription schedule is canceled because payment delinquency terminated the related subscription.
     * 由于付款拖欠终止相关订阅而取消订阅计划时发送
     */
    String SUBSCRIPTION_SCHEDULE_ABORTED = "subscription_schedule.aborted";
    /**
     * Sent when a subscription schedule is canceled, which also cancels any active associated subscription.
     */
    String SUBSCRIPTION_SCHEDULE_CANCELED = "subscription_schedule.canceled";
    /**
     * Occurs whenever a customer is signed up for a new plan
     */
    String CUSTOMER_SUBSCRIPTION_CREATED = "customer.subscription.created";
    /**
     * Occurs whenever a customer is signed up for a new plan
     */
    String CUSTOMER_SUBSCRIPTION_UPDATED = "customer.subscription.updated";
    /**
     * Sent when a customer’s subscription ends.
     */
    String CUSTOMER_SUBSCRIPTION_DELETED = "customer.subscription.deleted";
    /**
     * Occurs when a SetupIntent has been successfully completed.
     */
    String SETUP_INTENT_SUCCEEDED = "setup_intent.succeeded";
    /**
     * Occurs whenever any property of a customer changes.
     */
    String CUSTOMER_UPDATED = "customer.updated";
}
