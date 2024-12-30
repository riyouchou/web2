package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.entity.OrderDeviceEntity;

import java.util.List;

public interface IOrderDeviceService extends IService<OrderDeviceEntity> {
    List<OrderDeviceEntity> getDeviceList(String orderId);
}
