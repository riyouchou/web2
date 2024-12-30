package com.yx.web2.api.service.impl;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.dynamic.datasource.annotation.Slave;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import com.yx.pass.remote.pms.PmsRemoteAccountService;
import com.yx.pass.remote.pms.model.resp.account.TenantAccountResp;
import com.yx.web2.api.common.enums.AccountType;
import com.yx.web2.api.common.enums.InstalmentPaymentStatus;
import com.yx.web2.api.common.enums.OrderStatus;
import com.yx.web2.api.common.model.AccountModel;
import com.yx.web2.api.common.req.order.BillQueryReq;
import com.yx.web2.api.common.resp.PageResp;
import com.yx.web2.api.common.resp.order.BillListResp;
import com.yx.web2.api.common.resp.order.OrderInvoiceResp;
import com.yx.web2.api.common.resp.order.OrderReceiptResp;
import com.yx.web2.api.config.Web2ApiConfig;
import com.yx.web2.api.entity.ContractEntity;
import com.yx.web2.api.entity.OrderDeviceEntity;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.entity.OrderPaymentEntity;
import com.yx.web2.api.service.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.util.R;
import org.yx.lib.utils.util.StringUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@RefreshScope
public class BillServiceImpl implements IBillService {

    private final IOrderPaymentService orderPaymentService;
    private final IOrderService iOrderService;
    private final IOrderPaymentService iOrderPaymentService;
    private final IOrderDeviceService iOrderDeviceService;
    private final Web2ApiConfig web2ApiConfig;
    private final IContractService iContractService;
    private final PmsRemoteAccountService pmsRemoteAccountService;


    @Slave
    @Override
    public R<PageResp<BillListResp>> list(Long tenantId, AccountModel accountModel, BillQueryReq billQueryReq) {
        QueryWrapper<OrderPaymentEntity> lambdaQueryWrapper = Wrappers.query();
        lambdaQueryWrapper.eq("a.tenant_id", tenantId);
        if (accountModel.getAccountType().intValue() == AccountType.GAMING_BD.getValue()) {
            lambdaQueryWrapper.eq("a.bd_account_id", accountModel.getAccountId());
        }
        if (StringUtil.isNotBlank(billQueryReq.getOrderId())) {
            lambdaQueryWrapper.eq("a.order_id", billQueryReq.getOrderId());
        }
        if (billQueryReq.getPaymentStatus() != null) {
            lambdaQueryWrapper.eq("b.payment_status", billQueryReq.getPaymentStatus());
        }
        if (StringUtil.isBlank(billQueryReq.getStartTime())) {
            billQueryReq.setStartTime(DateUtil.formatDateTime(DateUtils.addDays(new Date(System.currentTimeMillis()), -7)));
        }
        if (StringUtil.isBlank(billQueryReq.getEndTime())) {
            billQueryReq.setEndTime(DateUtil.now());
        }
        lambdaQueryWrapper.eq("a.order_resource_pool", billQueryReq.getOrderResourcePool());
        lambdaQueryWrapper.ge("b.create_time", DateUtil.parseDateTime(billQueryReq.getStartTime()));
        lambdaQueryWrapper.le("b.create_time", DateUtil.parseDateTime(billQueryReq.getEndTime()));

        lambdaQueryWrapper.in("a.order_status", Stream.of(OrderStatus.AwaitingPaymentReceipt.getValue(), OrderStatus.NotStarted.getValue(), OrderStatus.InService.getValue(), OrderStatus.End.getValue(), OrderStatus.Terminated.getValue()).collect(Collectors.toList()));


        IPage<JSONObject> page = new Page<>(billQueryReq.getCurrent(), billQueryReq.getSize());
        page.orders().addAll(Lists.newArrayList(OrderItem.desc("b.order_id"), OrderItem.asc("b.id ")));
        page = orderPaymentService.billList(page, lambdaQueryWrapper);

        List<BillListResp> resultList = new ArrayList<>();
        for (JSONObject jsonObject : page.getRecords()) {
            resultList.add(
                    BillListResp.builder().orderId(jsonObject.getString("order_id"))
                            .paymentOrderId(jsonObject.getString("payment_order_id"))
                            .orderResourcePool(jsonObject.getInteger("order_resource_pool"))
                            .prePayment(jsonObject.getBoolean("pre_payment"))
                            .paymentStatus(jsonObject.getInteger("payment_status"))
                            .payTime(jsonObject.getTimestamp("pay_finish_time") == null ? "" : DateUtil.formatDateTime((Timestamp) jsonObject.getTimestamp("pay_finish_time")))
                            .instalmentMonth(jsonObject.getInteger("instalment_month"))
                            .instalmentMonthTotal(jsonObject.getInteger("instalment_month_total"))
                            .amount(jsonObject.getBigDecimal("hp_price").setScale(2, RoundingMode.UP).toString()).build());
        }

        PageResp<BillListResp> resultData = new PageResp<>();
        resultData.setCurrent(billQueryReq.getCurrent());
        resultData.setSize(billQueryReq.getSize());
        resultData.setRecords(resultList);
        resultData.setTotal(page.getTotal());
        resultData.setPages(page.getPages());
        return R.ok(resultData);
    }

    @Slave
    @Override
    public OrderInvoiceResp invoice(BillQueryReq billQueryReq) {
        OrderEntity order = iOrderService.getOrder(billQueryReq.getOrderId());
        boolean isArs = order.getOrderResourcePool() == NumberUtils.INTEGER_ONE ? true : false;
        ContractEntity contract = iContractService.getContractByOrderId(billQueryReq.getOrderId());
        OrderPaymentEntity orderPayment = iOrderPaymentService.getOrderPayment(billQueryReq.getPaymentOrderId());
        OrderInvoiceResp resp = new OrderInvoiceResp();

        resp.setClientName(isArs ? order.getTenantName() : contract.getCustomerLegalEntityName());
        resp.setClientAddress(isArs ? StringUtils.EMPTY : contract.getCustomerLegalEntityAddress());
        R<TenantAccountResp> ownerAccountInfo = pmsRemoteAccountService.getOwnerAccountInfo(order.getTenantId());
        TenantAccountResp data = ownerAccountInfo.getData();
        resp.setClientContactEmailAddress(data.getEmail());

        resp.setInvoiceNo(order.getOrderId() + "-" + orderPayment.getInstalmentMonth());
        resp.setPoNo(order.getOrderId());
        resp.setDate(orderPayment.getCreateTime());
        resp.setDueDate(orderPayment.getDueDate());

        resp.setTerms("Due on Receipt");

        resp.setInstalmentMonth(orderPayment.getInstalmentMonth());
        resp.setInstalmentMonthTotal(orderPayment.getInstalmentMonthTotal());
        resp.setDescription(isArs ? "Game-Basic" : "AI-Basic");
        resp.setTotalAmount(orderPayment.getHpPrice().add(ObjectUtils.defaultIfNull(orderPayment.getHpPrePaymentPrice(), BigDecimal.ZERO)));
        resp.setPrePaidAmount(orderPayment.getHpPrePaymentPrice());
        resp.setBalanceDue(orderPayment.getHpPrice());
        resp.setServiceBeginTime(orderPayment.getPeriodStart());
        resp.setServiceEndTime(orderPayment.getPeriodEnd());
        List<OrderDeviceEntity> deviceList = iOrderDeviceService.getDeviceList(billQueryReq.getOrderId());
        resp.setOrderDevices(deviceList);
        return resp;
    }

    @Override
    public OrderReceiptResp receipt(BillQueryReq billQueryReq) {
        OrderPaymentEntity orderPayment = iOrderPaymentService.getOrderPayment(billQueryReq.getPaymentOrderId());
        if (orderPayment.getPaymentStatus() != InstalmentPaymentStatus.PAID.getValue()) {
            throw new RuntimeException("The bill has not been paid");
        }
        OrderEntity order = iOrderService.getOrder(billQueryReq.getOrderId());
        boolean isArs = order.getOrderResourcePool() == NumberUtils.INTEGER_ONE ? true : false;
        ContractEntity contract = iContractService.getContractByOrderId(billQueryReq.getOrderId());
        OrderReceiptResp resp = new OrderReceiptResp();

        resp.setClientName(isArs ? order.getTenantName() : contract.getCustomerLegalEntityName());
        resp.setClientAddress(isArs ? StringUtils.EMPTY : contract.getCustomerLegalEntityAddress());
        resp.setClientContactEmailAddress(pmsRemoteAccountService.getOwnerAccountInfo(order.getTenantId()).getData().getEmail());
        resp.setPaymentDate(orderPayment.getPayFinishTime());
        resp.setPoNo(order.getOrderId());

        resp.setInvoiceNo(order.getOrderId() + "-" + orderPayment.getInstalmentMonth());

        resp.setReceiptNo(order.getOrderId() + "-" + orderPayment.getInstalmentMonth());

        resp.setTotalAmount(orderPayment.getHpPrice());
        resp.setPaidAmount(orderPayment.getHpPrice());
        resp.setDescription(isArs ? "Game-Basic" : "AI-Basic");
        resp.setInstalmentMonth(orderPayment.getInstalmentMonth());
        resp.setInstalmentMonthTotal(orderPayment.getInstalmentMonthTotal());
        resp.setBalanceDue(new BigDecimal("0"));
        resp.setServiceBeginTime(orderPayment.getPeriodStart());
        resp.setServiceEndTime(orderPayment.getPeriodEnd());
        if (orderPayment.getPrePayment() && orderPayment.getInstalmentMonth() == NumberUtils.INTEGER_ZERO) {
            resp.setInvoiceNo("Prepayment");
        } else {
            List<OrderDeviceEntity> deviceList = iOrderDeviceService.getDeviceList(billQueryReq.getOrderId());
            resp.setOrderDevices(deviceList);
        }

        return resp;
    }
}
