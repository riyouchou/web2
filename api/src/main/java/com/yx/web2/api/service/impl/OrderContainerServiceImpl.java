package com.yx.web2.api.service.impl;

import com.baomidou.dynamic.datasource.annotation.Master;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yx.web2.api.common.enums.OrderContainerStatus;
import com.yx.web2.api.common.enums.OrderStatus;
import com.yx.web2.api.entity.OrderContainerEntity;
import com.yx.web2.api.entity.OrderEntity;
import com.yx.web2.api.mapper.OrderContainerMapper;
import com.yx.web2.api.mapper.OrderMapper;
import com.yx.web2.api.service.IOrderContainerService;
import com.yx.web2.api.service.IOrderPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderContainerServiceImpl extends ServiceImpl<OrderContainerMapper, OrderContainerEntity> implements IOrderContainerService {

    private final OrderMapper orderMapper;
    private final IOrderPaymentService orderPaymentService;

    @Master
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderContainersStatus(OrderEntity orderEntity, List<Long> cids, int status) {
        update(Wrappers.lambdaUpdate(OrderContainerEntity.class)
                .set(OrderContainerEntity::getStatus, status)
                .eq(OrderContainerEntity::getOrderId, orderEntity.getOrderId())
                .eq(OrderContainerEntity::getWholesaleTid, orderEntity.getTenantId())
                .eq(OrderContainerEntity::getStatus, OrderContainerStatus.IN_SERVICE.getValue())
                .in(cids != null && !cids.isEmpty(), OrderContainerEntity::getCid, cids));

        if (status == OrderContainerStatus.ORDER_CLOSED_NORMALLY.getValue()
                && !Objects.equals(orderEntity.getOrderStatus(), OrderStatus.End.getValue())) {
            orderEntity.setOrderStatus(OrderStatus.End.getValue());
            orderMapper.updateById(orderEntity);
        } else if (status == OrderContainerStatus.CREDIT_INSUFFICIENT_CLOSED_ABNORMALLY.getValue()
                && !Objects.equals(orderEntity.getOrderStatus(), OrderStatus.InsufficientCredit.getValue())) {
            orderEntity.setOrderStatus(OrderStatus.InsufficientCredit.getValue());
            orderMapper.updateById(orderEntity);
            // update payment valid flag
            orderPaymentService.updateOrderPaymentValidFlag(orderEntity.getOrderId());
        } else if (status == OrderContainerStatus.ORDER_ABNORMALLY_CLOSED.getValue()
                && !Objects.equals(orderEntity.getOrderStatus(), OrderStatus.AbnormalCancel.getValue())) {
            orderEntity.setOrderStatus(OrderStatus.AbnormalCancel.getValue());
            orderMapper.updateById(orderEntity);
            // update payment valid flag
            orderPaymentService.updateOrderPaymentValidFlag(orderEntity.getOrderId());
        }
    }

}
