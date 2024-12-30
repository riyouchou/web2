package com.yx.web2.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@TableName("t_order_container")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderContainerEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long cid;

    private Long orderDeviceId;

    private String orderId;

    private String athOrderId;

    private Integer orderResourcePool;

    private Long wholesaleTid;

    private String spec;

    private String regionCode;

    private String wholesalePrice;

    private Integer status;

    private Timestamp serviceStartTime;

    private Timestamp serviceEndTime;

    private Integer refundStatus;

    private Timestamp refundTime;

    private BigDecimal realityRefundPrice;

    private String reason;

    private Timestamp createTime;

    private Timestamp lastUpdateTime;
}
