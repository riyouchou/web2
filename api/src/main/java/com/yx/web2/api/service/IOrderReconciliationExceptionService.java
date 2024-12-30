package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.common.req.order.OrderPayVirtualReq;
import com.yx.web2.api.entity.OrderReconciliationExceptionEntity;

import java.math.BigDecimal;

public interface IOrderReconciliationExceptionService extends IService<OrderReconciliationExceptionEntity> {
    void saveBatchOrderReconciliationException(OrderPayVirtualReq orderPayVirtualReq, BigDecimal amount, BigDecimal value, String to);
}