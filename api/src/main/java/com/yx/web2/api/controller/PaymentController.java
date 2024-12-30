package com.yx.web2.api.controller;

import com.yx.web2.api.common.constant.RequestAttributeConstants;
import com.yx.web2.api.common.enums.SysCode;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.payment.method.PaymentMethodBindReq;
import com.yx.web2.api.common.req.payment.method.PaymentMethodBindSuccessReq;
import com.yx.web2.api.service.ICustomerPaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringUtil;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final ICustomerPaymentMethodService paymentMethodService;

    @GetMapping("/method/get")
    public R<?> paymentMethodBind(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel) {
        return paymentMethodService.getPaymentMethod(tenantId, accountModel);
    }

    @PostMapping("/method/bind")
    public R<?> paymentMethodBind(
            @RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
            @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
            @RequestBody PaymentMethodBindReq paymentMethodBindReq) {
        return paymentMethodService.bindPaymentMethod(tenantId, accountModel, paymentMethodBindReq);
    }

    @PostMapping("/method/bindSuccess")
    public R<?> paymentMethodBindSuccess(@RequestAttribute(RequestAttributeConstants.TENANT_ID) Long tenantId,
                                         @RequestAttribute(RequestAttributeConstants.ACCOUNT_INFO) AccountModel accountModel,
                                         @RequestBody PaymentMethodBindSuccessReq paymentMethodBindSuccessReq) {
        if (StringUtil.isBlank(paymentMethodBindSuccessReq.getSetupIntentId())) {
            return R.failed(SysCode.x00000400.getValue(), SysCode.x00000400.getMsg());
        }
        return paymentMethodService.paymentMethodBindSuccess(tenantId, accountModel, paymentMethodBindSuccessReq);
    }
}
