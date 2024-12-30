package com.yx.web2.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@TableName("t_subscribed_service")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscribedServiceEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private String paymentOrderId;
    private String athOrderId;
    private Long tenantId;
    private Long accountId;
    private Timestamp serviceBeginTime;
    private Timestamp serviceEndTime;
    private Timestamp createTime;
    @TableField(exist = false)
    private String tenantName;
}
