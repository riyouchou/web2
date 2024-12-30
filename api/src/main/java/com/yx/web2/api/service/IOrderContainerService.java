package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.entity.OrderContainerEntity;
import com.yx.web2.api.entity.OrderEntity;

import java.util.List;

public interface IOrderContainerService extends IService<OrderContainerEntity> {
    void updateOrderContainersStatus(OrderEntity orderEntity, List<Long> cids, int status);
}