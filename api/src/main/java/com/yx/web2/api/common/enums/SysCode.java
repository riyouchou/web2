package com.yx.web2.api.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SysCode {
    x00000000("Success", 0),
    x00000400("Bad request", 143400),
    x00000401("Token timeout", 143401),
    x00000403("Unauthorized", 143403),
    x00000404("Not found", 143404),
    x00000405("Action not allow", 143405),
    x00000406("Data is exists", 143406),
    x00000407("The account has not been linked to a payment method", 143407),
    x00000410("Order is paid", 143410),
    x00000411("Order is not payment", 143411),
    x00000420("Create customer failed", 143420),
    x00000421("The payment method has been bound", 143421),
    x00000430("Get usd rate failed", 143430),
    x00000431("Transfer ath failed", 143431),
    x00000432("Close order failed", 143432),
    x00000433("Failed to create installment plan", 143433),
    x00000440("Contract operate need authorize", 143440),
    x00000441("Confirm paid failed, the contract not started", 143441),
    x00000442("Create order from contract failed", 143442),
    x00000443("The contract is reviewing", 143443),
    x00000449("Pay failed, the order status not pay", 143449),
    x00000450("Due pay failed, the order status changes sent", 143450),
    x00000451("Due pay failed, pay id is not unique", 143451),
    x00000452("The number of cids is inconsistent with totalQuantity.", 143452),
    x00000453("Get TenantInfo Failed", 143453),
    x00000454("Get cid result orderDeviceId is null or device not exist", 143454),
    x00000455("Virtual pre order size parameter is inconsistent with the query results", 143455),
    x00000456("Virtual pre orderPayment size parameter is inconsistent with the query results", 143456),
    x00000457("Virtual pre orderPayment price is inconsistent with the query results", 143457),
    x00000458("Virtual order size parameter is inconsistent with the query results", 143458),
    x00000459("Virtual orderPayment size parameter is inconsistent with the query results", 143459),
    x00000460("Virtual orderPayment price is inconsistent with the query results", 143460),
    x00000461("Virtual orderPayment is not exit", 143461),

    x00000801("Create contract exception", 143801),
    x00000802("Contract review exception", 143802),
    x00000803("Bind payment exception", 143803),
    x00000804("Payment bind Success exception", 143804),
    x00000805("Order pay exception", 143805),
    x00000806("Due pay exception", 143806),
    x00000807("Virtual pre due pay exception", 143807),
    x00000808("Virtual due pay exception", 143808),

    x00000601("Call fee center create order, result is null", 143601),
    x00000602("Call fee center transfer ath, result is null", 143602),
    x00000603("Call pms pay order, result is null", 143603),
    x00000604("Call feeCenter bind container, result is null", 143604),

    x00000010("Request too fast", 143010),
    ;

    private final String msg;
    private final int value;
}