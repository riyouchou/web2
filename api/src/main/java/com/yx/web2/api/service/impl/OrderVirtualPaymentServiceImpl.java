package com.yx.web2.api.service.impl;

import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yx.web2.api.common.constant.Web2LoggerEvents;
import com.yx.web2.api.common.enums.InstalmentPaymentStatus;
import com.yx.web2.api.common.enums.OrderPaymentStatus;
import com.yx.web2.api.common.enums.OrderStatus;
import com.yx.web2.api.common.req.order.OrderPayVirtualReq;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.entity.OrderPaymentEntity;
import com.yx.web2.api.entity.OrderVirtualPaymentEntity;
import com.yx.web2.api.mapper.OrderVirtualPaymentMapper;
import com.yx.web2.api.service.IOrderContainerService;
import com.yx.web2.api.service.IOrderPaymentService;
import com.yx.web2.api.service.IOrderService;
import com.yx.web2.api.service.IOrderVirtualPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yx.lib.utils.logger.KvLogger;
import org.yx.lib.utils.logger.LogFieldConstants;
import org.yx.lib.utils.util.SpringContextHolder;

import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderVirtualPaymentServiceImpl extends ServiceImpl<OrderVirtualPaymentMapper, OrderVirtualPaymentEntity> implements IOrderVirtualPaymentService {

    private final IOrderPaymentService orderPaymentService;

    @Master
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePaymentRecordSuccess(OrderPayVirtualReq.OrderPay orderPay) {
        IOrderService orderService = SpringContextHolder.getBean(IOrderService.class);
        //query all order payment
        List<OrderPaymentEntity> orderPaymentEntities = orderPaymentService.list(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .in(OrderPaymentEntity::getPaymentOrderId, orderPay.getOrderPaymentIdList()));
        boolean isPrePayment = orderPaymentEntities.stream().anyMatch(OrderPaymentEntity::getPrePayment);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        OrderEntity orderEntity = orderService.getOrder(orderPay.getOrderId());
        KvLogger.instance(OrderVirtualPaymentServiceImpl.class)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_RECORD_SUCCESS)
                .p("OrderId", orderPay.getOrderId())
                .p("OrderStatus", orderEntity.getOrderStatus())
                .p("OrderPaymentStatus", orderEntity.getPaymentStatus())
                .p("PrePayment", isPrePayment)
                .i();
        // update order
        if (isPrePayment) {
            orderService.update(OrderEntity.builder()
                            .orderStatus(OrderStatus.AwaitingPaymentReceipt.getValue())
                            .paymentStatus(OrderPaymentStatus.NormalPayment.getValue())
                            .lastUpdateTime(now)
                            .build(),
                    Wrappers.lambdaUpdate(OrderEntity.class)
                            .eq(OrderEntity::getOrderId, orderPay.getOrderId()));
        } else {
            orderService.update(OrderEntity.builder()
                            .paymentStatus(OrderPaymentStatus.NormalPayment.getValue())
                            .lastUpdateTime(now)
                            .build(),
                    Wrappers.lambdaUpdate(OrderEntity.class)
                            .eq(OrderEntity::getOrderId, orderPay.getOrderId()));
        }
        //update order payment
        orderPaymentService.update(OrderPaymentEntity.builder()
                        .payFinishTime(now)
                        .lastUpdateTime(now)
                        .paymentStatus(InstalmentPaymentStatus.PAID.getValue())
                        .build(),
                Wrappers.lambdaQuery(OrderPaymentEntity.class)
                        .in(OrderPaymentEntity::getPaymentOrderId, orderPay.getOrderPaymentIdList()));
        //update virtual payment
        update(OrderVirtualPaymentEntity.builder()
                        .lastUpdateTime(now)
                        .status(InstalmentPaymentStatus.None.getValue())
                        .build(),
                Wrappers.lambdaQuery(OrderVirtualPaymentEntity.class)
                        .in(OrderVirtualPaymentEntity::getPaymentOrderId, orderPay.getOrderPaymentIdList()));
    }


    @Master
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePaymentRecordFail(OrderPayVirtualReq.OrderPay orderPay) {
        IOrderService orderService = SpringContextHolder.getBean(IOrderService.class);
        //query all order payment
        List<OrderPaymentEntity> orderPaymentEntities = orderPaymentService.list(Wrappers.lambdaQuery(OrderPaymentEntity.class)
                .in(OrderPaymentEntity::getPaymentOrderId, orderPay.getOrderPaymentIdList()));
        boolean isPrePayment = orderPaymentEntities.stream().anyMatch(OrderPaymentEntity::getPrePayment);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        OrderEntity orderEntity = orderService.getOrder(orderPay.getOrderId());
        KvLogger.instance(OrderVirtualPaymentServiceImpl.class)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.VIRTUAL_PAY_ORDER_EVENT_ACTION_RECORD_FAIL)
                .p("OrderId", orderPay.getOrderId())
                .p("OrderStatus", orderEntity.getOrderStatus())
                .p("OrderPaymentStatus", orderEntity.getPaymentStatus())
                .p("PrePayment", isPrePayment)
                .i();
        // update order
        if (isPrePayment) {
            orderService.update(OrderEntity.builder()
                            .orderStatus(OrderStatus.WaitingPayment.getValue())
                            .paymentStatus(OrderPaymentStatus.NormalPaymentFailed.getValue())
                            .lastUpdateTime(now)
                            .build(),
                    Wrappers.lambdaUpdate(OrderEntity.class)
                            .eq(OrderEntity::getOrderId, orderPay.getOrderId()));

        } else {
            orderService.update(OrderEntity.builder()
                            .paymentStatus(OrderPaymentStatus.OverduePayment.getValue())
                            .lastUpdateTime(now)
                            .build(),
                    Wrappers.lambdaUpdate(OrderEntity.class)
                            .eq(OrderEntity::getOrderId, orderPay.getOrderId()));
        }

        //update order payment
        orderPaymentService.update(OrderPaymentEntity.builder()
                        .payFinishTime(now)
                        .lastUpdateTime(now)
                        .paymentStatus(InstalmentPaymentStatus.FAILED.getValue())
                        .build(),
                Wrappers.lambdaQuery(OrderPaymentEntity.class)
                        .in(OrderPaymentEntity::getPaymentOrderId, orderPay.getOrderPaymentIdList()));

        //update virtual payment
        update(OrderVirtualPaymentEntity.builder()
                        .lastUpdateTime(now)
                        .status(InstalmentPaymentStatus.FAILED.getValue())
                        .build(),
                Wrappers.lambdaQuery(OrderVirtualPaymentEntity.class)
                        .in(OrderVirtualPaymentEntity::getPaymentOrderId, orderPay.getOrderPaymentIdList()));
    }

    @Master
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveVirtualPaymentStatusSuccess(OrderPayVirtualReq.OrderPay orderPay) {
        IOrderService orderService = SpringContextHolder.getBean(IOrderService.class);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        OrderEntity orderEntity = orderService.getOrder(orderPay.getOrderId());
        KvLogger.instance(OrderVirtualPaymentServiceImpl.class)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SAVE_VIRTUAL_PAYMENT_STATUS)
                .p("OrderId", orderPay.getOrderId())
                .p("OrderStatus", orderEntity.getOrderStatus())
                .p("OrderPaymentStatus", orderEntity.getPaymentStatus())
                .i();
        //update virtual payment
        update(OrderVirtualPaymentEntity.builder()
                        .lastUpdateTime(now)
                        .status(InstalmentPaymentStatus.PAID.getValue())
                        .build(),
                Wrappers.lambdaQuery(OrderVirtualPaymentEntity.class)
                        .in(OrderVirtualPaymentEntity::getPaymentOrderId, orderPay.getOrderPaymentIdList()));
    }

    @Master
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveVirtualPaymentStatusFail(OrderPayVirtualReq.OrderPay orderPay) {
        IOrderService orderService = SpringContextHolder.getBean(IOrderService.class);
        Timestamp now = new Timestamp(System.currentTimeMillis());

        OrderEntity orderEntity = orderService.getOrder(orderPay.getOrderId());
        KvLogger.instance(OrderVirtualPaymentServiceImpl.class)
                .p(LogFieldConstants.EVENT, Web2LoggerEvents.VIRTUAL_PAY_ORDER_JOB_EVENT)
                .p(LogFieldConstants.ACTION, Web2LoggerEvents.Actions.SAVE_VIRTUAL_PAYMENT_STATUS)
                .p("OrderId", orderPay.getOrderId())
                .p("OrderStatus", orderEntity.getOrderStatus())
                .p("OrderPaymentStatus", orderEntity.getPaymentStatus())
                .i();
        //update virtual payment
        update(OrderVirtualPaymentEntity.builder()
                        .lastUpdateTime(now)
                        .status(InstalmentPaymentStatus.FAILED.getValue())
                        .build(),
                Wrappers.lambdaQuery(OrderVirtualPaymentEntity.class)
                        .in(OrderVirtualPaymentEntity::getPaymentOrderId, orderPay.getOrderPaymentIdList()));
    }

}