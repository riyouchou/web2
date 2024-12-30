package com.yx.web2.api.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yx.web2.api.entity.OrderDeviceEntity;
import com.yx.web2.api.mapper.OrderDeviceMapper;
import com.yx.web2.api.service.IOrderDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderDeviceServiceImpl extends ServiceImpl<OrderDeviceMapper, OrderDeviceEntity> implements IOrderDeviceService {
    @Override
    public List<OrderDeviceEntity> getDeviceList(String orderId) {
        return this.list(Wrappers.lambdaQuery(OrderDeviceEntity.class).eq(OrderDeviceEntity::getOrderId, orderId));
    }
}
