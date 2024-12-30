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
@TableName("t_order")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private BigDecimal initialPrice;
    private BigDecimal currentPrice;
    private BigDecimal publishPrice;
    private BigDecimal prePaymentPrice;
    private Integer serviceDuration;
    private Integer serviceDurationPeriod;
    private Boolean autoRenew;
    private Integer instalmentMonthTotal;
    private BigDecimal instalmentMonthPaymentAvg;
    private String subscriptionId;
    private String scheduleId;
    private Integer orderStatus;
    private Integer paymentStatus;
    private String failureReason;
    private String customerId;
    private String redirectUrl;
    private Long accountId;
    private String accountName;
    private Long bdAccountId;
    private String bdAccountName;
    private Long financeAccountId;
    private String financeAccountName;
    private Long tenantId;
    private String tenantName;
    private Timestamp createTime;
    private Timestamp lastUpdateTime;
    private Long lastUpdateAccountId;
    private Boolean deleted;
    private Integer orderResourcePool;
    private String reason;
}