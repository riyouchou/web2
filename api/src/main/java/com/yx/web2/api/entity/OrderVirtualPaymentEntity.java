package com.yx.web2.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@TableName("t_order_virtual_payment")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderVirtualPaymentEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private String paymentOrderId;
    private Long tenantId;
    private String hashCode;
    private String fromAddress;
    private String toAddress;
    private String amount;
    private Integer type;
    private Integer status;
    private Timestamp createTime;
    private Timestamp lastUpdateTime;

}