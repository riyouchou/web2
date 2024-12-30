package com.yx.web2.api.service.impl;

import cn.hutool.core.map.MapBuilder;
import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.dynamic.datasource.annotation.Slave;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.SetupIntent;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.SysCode;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.payment.method.PaymentMethodBindReq;
import com.yx.web2.api.common.req.payment.method.PaymentMethodBindSuccessReq;
import com.yx.web2.api.entity.CustomerPaymentMethodEntity;
import com.yx.web2.api.mapper.CustomerPaymentMethodMapper;
import com.yx.web2.api.service.ICustomerPaymentMethodService;
import com.yx.web2.api.service.payment.StripePaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.SpringContextHolder;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerPaymentMethodServiceImpl extends ServiceImpl<CustomerPaymentMethodMapper, CustomerPaymentMethodEntity> implements ICustomerPaymentMethodService {

    private final StripePaymentService stripePaymentService;

    @Master
    @Override
    public R<?> bindPaymentMethod(Long tenantId, AccountModel accountModel, PaymentMethodBindReq paymentMethodBindReq) {
        if (accountModel.getOwnerAccountId() == null) {
            return R.failed(SysCode.x00000404.getValue(), "not found owner");
        }
        KvLogger kvLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ACCOUNT_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ACCOUNT_EVENT_ACTION_BIND_PAYMENT_METHOD)
                .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                .p(LogFieldConstants.ACCOUNT_NAME, accountModel.getAccountName())
                .p(LogFieldConstants.TENANT_ID, tenantId)
                .p("OwnerAccountId", accountModel.getOwnerAccountId())
                .p("OwnerAccountName", accountModel.getOwnerAccountName())
                .p("OwnerAccountEmail", accountModel.getOwnerAccountEmail());
        Customer customer;
        try {
            customer = stripePaymentService.getCustomer(accountModel.getOwnerAccountName(), accountModel.getOwnerAccountEmail());
            if (customer == null) {
                customer = stripePaymentService.createCustomer(accountModel.getOwnerAccountName(), accountModel.getOwnerAccountEmail());
            }
            kvLogger.p("customerId", customer.getId());

            SetupIntent setupIntent = stripePaymentService.createSetupIntent(
                    StripePaymentService.CreateSetupIntentParams.builder()
                            .customerId(customer.getId())
                            .paymentMethodTypes(Lists.newArrayList("card", "us_bank_account"))
                            .accountId(accountModel.getOwnerAccountId())
                            .tenantId(accountModel.getTenantId())
                            .build());

            kvLogger.p("bindId", setupIntent.getClientSecret())
                    .i();
            return R.ok(MapBuilder.create(new HashMap<String, String>()).put("bindId", setupIntent.getClientSecret()).build());
        } catch (StripeException e) {
            kvLogger.p(LogFieldConstants.ERR_MSG, e.getMessage()).e(e);
            return R.failed(SysCode.x00000803.getValue(), SysCode.x00000803.getMsg());
        }
    }

    @Slave
    @Override
    public CustomerPaymentMethodEntity getSimple(Long tenantId) {
        return getOne(Wrappers.lambdaQuery(CustomerPaymentMethodEntity.class)
                .eq(CustomerPaymentMethodEntity::getTenantId, tenantId)
                .select(
                        CustomerPaymentMethodEntity::getId,
                        CustomerPaymentMethodEntity::getAccountId,
                        CustomerPaymentMethodEntity::getCustomerId,
                        CustomerPaymentMethodEntity::getPaymentMethodId,
                        CustomerPaymentMethodEntity::getLastUpdateTime
                )
                .last(" LIMIT 1"));
    }

    @Override
    public CustomerPaymentMethodEntity getSimple(String customerId) {
        return getOne(Wrappers.lambdaQuery(CustomerPaymentMethodEntity.class)
                .eq(CustomerPaymentMethodEntity::getCustomerId, customerId)
                .select(
                        CustomerPaymentMethodEntity::getId,
                        CustomerPaymentMethodEntity::getAccountId,
                        CustomerPaymentMethodEntity::getCustomerId,
                        CustomerPaymentMethodEntity::getPaymentMethodId,
                        CustomerPaymentMethodEntity::getLastUpdateTime
                )
                .last(" LIMIT 1"));
    }

    @Override
    public R<?> paymentMethodBindSuccess(Long tenantId, AccountModel accountModel, PaymentMethodBindSuccessReq paymentMethodBindSuccessReq) {
        // 绑定支付方式成功后，客户端会主动调用此方法，stripe的webhook也会通知绑定支付方式成功，
        // 由于webhook通知会晚于客户端，所以当客户端收到回调后，先通知，入库时靠唯一索引保证不重复
        KvLogger kvLogger = KvLogger.instance(this)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.ACCOUNT_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.ACCOUNT_EVENT_ACTION_BIND_PAYMENT_METHOD_SUCCESS)
                .p(LogFieldConstants.ACCOUNT_ID, accountModel.getAccountId())
                .p(LogFieldConstants.TENANT_ID, tenantId);
        ICustomerPaymentMethodService customerPaymentMethodService = SpringContextHolder.getBean(ICustomerPaymentMethodService.class);
        try {
            SetupIntent setupIntent = SetupIntent.retrieve(paymentMethodBindSuccessReq.getSetupIntentId());
            String customerId = setupIntent.getCustomer();

            kvLogger.p("setupIntentId", paymentMethodBindSuccessReq.getSetupIntentId())
                    .p("customerId", customerId)
                    .p("tenantId", tenantId)
                    .p("ownerAccountId", accountModel.getOwnerAccountId());

            String paymentMethodId = setupIntent.getPaymentMethod();
            kvLogger.p("paymentMethodId", paymentMethodId);

            PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodId);
            // 设置默认支付方式
            stripePaymentService.setCustomerDefaultPaymentMethod(customerId, paymentMethodId);
            // 删除其他支付方式
            List<PaymentMethod> paymentMethodList = stripePaymentService.listPaymentMethod(customerId);
            if (paymentMethodList != null && !paymentMethodList.isEmpty()) {
                for (PaymentMethod customerPaymentMethod : paymentMethodList) {
                    if (!customerPaymentMethod.getId().equals(paymentMethodId)) {
                        kvLogger.p("paymentMethodDetach", customerPaymentMethod.getId());
                        customerPaymentMethod.detach();
                    }
                }
            }
            // 修改数据库
            CustomerPaymentMethodEntity customerPaymentMethodEntity = customerPaymentMethodService.getSimple(customerId);
            if (customerPaymentMethodEntity == null) {
                customerPaymentMethodService.save(CustomerPaymentMethodEntity.builder()
                        .customerId(customerId)
                        .tenantId(accountModel.getTenantId())
                        .accountId(accountModel.getOwnerAccountId())
                        .paymentMethodId(paymentMethodId)
                        .paymentMethodInfo(JSON.toJSONString(paymentMethod))
                        .lastUpdateTime(new Timestamp(System.currentTimeMillis()))
                        .build());
                kvLogger.p("created", true).i();
            } else {
                kvLogger.p("oldPaymentMethodId", customerPaymentMethodEntity.getPaymentMethodId());
                customerPaymentMethodService.update(new LambdaUpdateWrapper<>(CustomerPaymentMethodEntity.class)
                        .set(CustomerPaymentMethodEntity::getPaymentMethodId, paymentMethodId)
                        .set(CustomerPaymentMethodEntity::getCustomerId, customerId)
                        .set(CustomerPaymentMethodEntity::getPaymentMethodInfo, JSON.toJSONString(paymentMethod))
                        .set(CustomerPaymentMethodEntity::getLastUpdateTime, new Timestamp(System.currentTimeMillis()))
                        .eq(CustomerPaymentMethodEntity::getId, customerPaymentMethodEntity.getId()));
                kvLogger.p("updated", true)
                        .p("newPaymentMethodId", paymentMethodId)
                        .i();
            }
            return R.ok();
        } catch (Exception ex) {
            kvLogger.p(LogFieldConstants.ERR_MSG, ex.getMessage())
                    .e(ex);
            return R.failed(SysCode.x00000804.getValue(), SysCode.x00000804.getMsg());
        }
    }

    @Override
    public R<?> getPaymentMethod(Long tenantId, AccountModel accountModel) {
        CustomerPaymentMethodEntity customerPaymentMethodEntity = getOne(Wrappers.lambdaQuery(CustomerPaymentMethodEntity.class)
                .eq(CustomerPaymentMethodEntity::getAccountId, accountModel.getOwnerAccountId())
                .eq(CustomerPaymentMethodEntity::getTenantId, tenantId)
                .select(
                        CustomerPaymentMethodEntity::getPaymentMethodInfo
                )
                .last(" LIMIT 1"));
        if (customerPaymentMethodEntity == null) {
            return R.ok();
        } else {
            return R.ok(JSON.parseObject(customerPaymentMethodEntity.getPaymentMethodInfo()));
        }
    }
}
