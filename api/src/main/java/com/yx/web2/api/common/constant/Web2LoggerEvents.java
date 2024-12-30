package com.yx.web2.api.common.constant;

public interface Web2LoggerEvents {
    String APPLICATION_RUNNER_EVENT = "ApplicationRunner";
    String TOKEN_FILTER_EVENT = "TokenFilterEvent";
    String SCAN_SERVICE_END_ORDER_EVENT = "ScanServiceEndOrderEvent";
    String TRANSFER_ATH_EVENT = "TransferAthEvent";
    String ORDER_EVENT = "OrderEvent";
    String HANDLE_WEB_HOOK_EVENT = "HandleWebHookEvent";
    String ATH_ORDER_EVENT = "AthOrderEvent";
    String ACCOUNT_EVENT = "AccountEvent";
    String CONTRACT_EVENT = "ContractEvent";
    String SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT = "ScanContractDeviceToCreateOrderEvent";
    String INSTALLMENT_CALCULATE_EVENT = "InstallmentCalculateEvent";
    String IDC_PICK_ORDER_CONTAINER_CONSUMER = "IdcPickOrderContainerConsumer";
    String VIRTUAL_PAY_ORDER_JOB_EVENT = "VirtualPayOrderJobEvent";
    String VIRTUAL_PAY_EVENT = "VirtualPayEvent";


    interface Actions {
        String APPLICATION_RUNNER_EVENT_INIT = "Init";

        String TOKEN_FILTER_EVENT_ACTION_INIT = "Init";
        String TOKEN_FILTER_EVENT_ACTION_DO_FILTER = "DoFilter";
        String TOKEN_FILTER_EVENT_ACTION_DESTROY = "Destroy";


        String SCAN_SERVICE_END_ORDER_EVENT_ACTION_START = "StartScan";
        String SCAN_SERVICE_END_ORDER_EVENT_ACTION_END = "EndScan";
        String SCAN_SERVICE_END_ORDER_EVENT_ACTION_SELECT_SERVICE_END_ORDERS = "SelectServiceEndOrders";
        String SCAN_SERVICE_END_ORDER_EVENT_ACTION_UPDATE_ORDER = "UpdateOrder";

        String HANDLE_WEB_HOOK_EVENT_ACTION_PROCESS_ORDER = "ProcessOrder";

        String SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_START = "StartScam";
        String SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_END = "EndScan";
        String SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_SELECT_CONTRACT = "SelectContract";
        String SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_SELECT_END_CONTRACT = "SelectEndContract";
        String SCAN_CONTRACT_DEVICE_TO_CREATE_ORDER_EVENT_ACTION_SELECT_CONTRACT_CONFIRM_PAID = "ConfirmPaid";

        String VIRTUAL_PAY_ORDER_EVENT_ACTION_START = "VirtualPayOrderEventStartScam";
        String VIRTUAL_PAY_ORDER_EVENT_ACTION_END = "VirtualPayOrderEventEndScan";
        String VIRTUAL_PAY_ORDER_EVENT_ACTION_SELECT = "VirtualPayOrderEventSelect";
        String VIRTUAL_PAY_ORDER_EVENT_ACTION_RECORD_SUCCESS = "savePaymentRecordSuccess";
        String VIRTUAL_PAY_ORDER_EVENT_ACTION_RECORD_FAIL = "savePaymentRecordFail";
        String SAVE_VIRTUAL_PAYMENT_STATUS = "saveVirtualPaymentStatus";


        String TRANSFER_ATH_EVENT_ACTION_START = "StartTransfer";
        String TRANSFER_ATH_EVENT_ACTION_END = "EndTransfer";
        String TRANSFER_ATH_EVENT_ACTION_START_ACTION_SELECT_IN_SERVICE_ORDERS = "SelectInServiceOrders";
        String TRANSFER_ATH_EVENT_ACTION_START_ACTION_SELECT_ATH_ORDERS = "SelectAthOrders";
        String TRANSFER_ATH_EVENT_ACTION_START_ACTION_TRANSFER_ATH = "TransferAth";


        String ORDER_EVENT_ACTION_WEB_HOOK_CALL_BACK = "WebhookCallback";
        String ORDER_EVENT_ACTION_PREPARE_ORDER_INSTALMENT_PLAN = "PrepareOrderInstalmentPlan";
        String ORDER_EVENT_ACTION_PURCHASE_SCHEDULE_CREATED = "PurchaseScheduleCreated";
        String ORDER_EVENT_ACTION_PURCHASE_SCHEDULE_CANCEL = "PurchaseScheduleCancel";
        String ORDER_EVENT_ACTION_CREATE_ORDER = "CreateOrder";
        String ORDER_EVENT_ACTION_CREATE_ORDER_BY_CONTRACT = "CreateOrderByContract";
        String ORDER_EVENT_ACTION_CREATE_PRE_PAYMENT = "PrePayment";
        String ORDER_EVENT_ACTION_PAY = "Pay";
        String ORDER_EVENT_ACTION_DUE_PAY = "DuePay";
        String ORDER_EVENT_ACTION_VIRTUAL_PAY = "PayVirtualPre";
        String ORDER_EVENT_ACTION_BD_CONFIRM_PRICE = "BdConfirmPrice";
        String ORDER_EVENT_ACTION_FINANCE_PUBLISH_PRICE = "FinancePublishPrice";
        String ORDER_EVENT_ACTION_FINANCE_CONFIRM_PAID = "FinanceConfirmPaid";
        String ORDER_EVENT_ACTION_FINANCE_FORCE_CONFIRM_PAID = "FinanceForceConfirmPaid";
        String ORDER_EVENT_ACTION_FINANCE_CONFIRM_PAID_CONTRACT_NOT_START = "FinanceConfirmPaidContractNotStart";
        String ORDER_EVENT_ACTION_DELETE = "Delete";
        String ORDER_EVENT_ACTION_TERMINATE = "Terminate";
        String ORDER_EVENT_ACTION_GET_SPECNAME = "GetSpecName";


        String ATH_ORDER_EVENT_BIND_CONTAINER = "BindContainer";
        String ATH_ORDER_EVENT_ACTION_CLOSE_ORDER = "CloseOrder";
        String ATH_ORDER_EVENT_ACTION_GET_USD_RATE = "GetUsdRate";
        String ATH_ORDER_EVENT_ACTION_CREATE_ORDER = "CreateOrder";
        String ATH_ORDER_EVENT_ACTION_TRANSFER_ATH = "TransferAth";
        String ATH_ORDER_EVENT_ACTION_PAY_ORDER = "PayOrder";
        String ATH_ORDER_EVENT_ACTION_BIND_CONTAINER = "BindContainer";
        String ATH_ORDER_EVENT_ACTION_CALCULATE_ATH_VALUE = "CalculateAthValue";
        String ATH_ORDER_EVENT_ACTION_CANCEL_SUBSCRIPTION_SCHEDULE = "CancelSubscriptionSchedule";
        String ATH_ORDER_EVENT_ACTION_CANCEL_SUBSCRIPTION = "CancelSubscription";


        String ACCOUNT_EVENT_ACTION_BIND_PAYMENT_METHOD = "BindPaymentMethod";
        String ACCOUNT_EVENT_ACTION_BIND_PAYMENT_METHOD_SUCCESS = "BindPaymentSuccess";

        String CONTRACT_EVENT_ACTION_CREATE = "CreateContract";
        String CONTRACT_EVENT_ACTION_UPDATE = "UpdateContract";
        String CONTRACT_EVENT_ACTION_REVIEW = "ContractReview";
        String CONTRACT_EVENT_ACTION_DELETE = "ContractDelete";
        String CONTRACT_EVENT_ACTION_SIGNED = "ContractSigned";
        String CONTRACT_EVENT_ACTION_CREATE_TENANT_CONTRACT_PATH = "CreateTenantContractPath";
        String CONTRACT_EVENT_ACTION_CREATE_TENANT_CONTRACT_FILE = "CreateTenantContractFile";
        String CONTRACT_EVENT_ACTION_CREATE_TENANT_CONTRACT_REPLACE_DATA = "CreateTenantContractReplaceData";

        String CONTRACT_EVENT_ACTION_DOCUSIGN_GET_ENVELOPE_PREVIEW_URL = "DocusignGetEnvelopePreviewUrl";
        String CONTRACT_EVENT_ACTION_DOCUSIGN_GET_ENVELOPE = "DocusignGetEnvelope";
        String CONTRACT_EVENT_ACTION_DOCUSIGN_GET_DOCUMENT = "DocusignGetDocument";
        String CONTRACT_EVENT_ACTION_DOCUSIGN_CREATE_ENVELOPE = "DocusignCreateEnvelope";
        String CONTRACT_EVENT_ACTION_DOCUSIGN_UNAUTHORIZE = "DocusignUnAuthorize";
        String CONTRACT_EVENT_ACTION_DOCUSIGN_CALLBACK_RECIPIENT_COMPLETED = "RecipientCompleted";

        String INSTALLMENT_CALCULATE_EVENT_CALCULATE = "Calculate";

        String KAFKA_RECEIVE_WEB2_PRICK_ORDER_CONTAINER = "KafkaReceiveWeb2PrickOrderContainer";
    }
}
