package com.yx.web2.api.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yx.web2.api.common.req.order.OrderPayVirtualReq;
import com.yx.web2.api.entity.OrderVirtualPaymentEntity;

public interface IOrderVirtualPaymentService extends IService<OrderVirtualPaymentEntity> {
    void savePaymentRecordSuccess(OrderPayVirtualReq.OrderPay orderPay);

    void savePaymentRecordFail(OrderPayVirtualReq.OrderPay orderPay);

    void saveVirtualPaymentStatusSuccess(OrderPayVirtualReq.OrderPay orderPay);

    void saveVirtualPaymentStatusFail(OrderPayVirtualReq.OrderPay orderPay);
}