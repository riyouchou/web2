package com.yx.web2.api.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yx.web2.api.common.req.order.OrderPayVirtualReq;
import com.yx.web2.api.entity.OrderPaymentEntity;
import com.yx.web2.api.entity.OrderReconciliationExceptionEntity;
import com.yx.web2.api.mapper.OrderReconciliationExceptionMapper;
import com.yx.web2.api.service.IOrderPaymentService;
import com.yx.web2.api.service.IOrderReconciliationExceptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.yx.lib.utils.util.SpringContextHolder;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderReconciliationExceptionServiceImpl extends ServiceImpl<OrderReconciliationExceptionMapper, OrderReconciliationExceptionEntity> implements IOrderReconciliationExceptionService {

    private final IOrderPaymentService orderPaymentService;

    @Override
    public void saveBatchOrderReconciliationException(OrderPayVirtualReq orderPayVirtualReq, BigDecimal amount, BigDecimal value, String to) {
        List<String> duePaymentOrderIds = orderPayVirtualReq.getOrderPayList().stream().map(OrderPayVirtualReq.OrderPay::getOrderPaymentIdList).flatMap(Collection::stream).collect(Collectors.toList());
        List<OrderPaymentEntity> orderPaymentEntities = orderPaymentService.list(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .in(OrderPaymentEntity::getPaymentOrderId, duePaymentOrderIds));

        List<OrderReconciliationExceptionEntity> orderReconciliationExceptionEntityList = new ArrayList<>();
        Map<String, OrderReconciliationExceptionEntity> existingOrderReconciliationExceptions = list(Wrappers.lambdaQuery(OrderReconciliationExceptionEntity.class)
                .in(OrderReconciliationExceptionEntity::getPaymentOrderId, duePaymentOrderIds))
                .stream()
                .collect(Collectors.toMap(OrderReconciliationExceptionEntity::getPaymentOrderId, entity -> entity,
                        (existing, replacement) -> existing));
        for (OrderPaymentEntity orderPaymentEntity : orderPaymentEntities) {
            String paymentOrderId = orderPaymentEntity.getPaymentOrderId();
            OrderReconciliationExceptionEntity existingPayment = existingOrderReconciliationExceptions.get(paymentOrderId);
            if (existingPayment == null) {
                orderReconciliationExceptionEntityList.add(OrderReconciliationExceptionEntity.builder()
                        .paymentOrderId(orderPaymentEntity.getPaymentOrderId())
                        .amount(amount.toPlainString())
                        .paymentRequired(value.toPlainString())
                        .toAddress(to)
                        .createTime(new Timestamp(System.currentTimeMillis()))
                        .lastUpdateTime(new Timestamp(System.currentTimeMillis()))
                        .build());
            } else {
                existingPayment.setAmount(amount.toPlainString());
                existingPayment.setPaymentRequired(value.toPlainString());
                existingPayment.setToAddress(to);
                existingPayment.setLastUpdateTime(new Timestamp(System.currentTimeMillis()));
                orderReconciliationExceptionEntityList.add(existingPayment);
            }
            SpringContextHolder.getBean(IOrderReconciliationExceptionService.class).saveOrUpdateBatch(orderReconciliationExceptionEntityList);
        }
    }
}