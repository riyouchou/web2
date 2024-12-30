package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stripe.model.PaymentIntent;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.payment.method.PaymentMethodBindReq;
import com.yx.web2.api.common.req.payment.method.PaymentMethodBindSuccessReq;
import com.yx.web2.api.entity.CustomerPaymentMethodEntity;
import org.yx.lib.utils.util.R;

public interface ICustomerPaymentMethodService extends IService<CustomerPaymentMethodEntity> {

    /**
     * 绑定支付方式
     *
     * @param tenantId             租户Id
     * @param accountModel         登录账户信息
     * @param paymentMethodBindReq 支付方式绑定参数
     * @return
     */
    R<?> bindPaymentMethod(Long tenantId, AccountModel accountModel, PaymentMethodBindReq paymentMethodBindReq);

    CustomerPaymentMethodEntity getSimple(Long tenantId);

    CustomerPaymentMethodEntity getSimple(String customerId);

    R<?> paymentMethodBindSuccess(Long tenantId, AccountModel accountModel, PaymentMethodBindSuccessReq paymentMethodBindSuccessReq);

    /**
     * 获取账户Owner已绑定的支付方式
     *
     * @param tenantId     租户Id
     * @param accountModel 账户信息
     * @return
     */
    R<?> getPaymentMethod(Long tenantId, AccountModel accountModel);
}
