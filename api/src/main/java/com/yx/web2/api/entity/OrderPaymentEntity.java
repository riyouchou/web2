package com.yx.web2.api.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@TableName("t_order_payment")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderPaymentEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderId;
    private String paymentOrderId;
    private Integer instalmentMonth;
    private Integer instalmentMonthTotal;
    private BigDecimal hpPrice;
    private BigDecimal hpPrePaymentPrice;
    private Integer paymentStatus;
    private String payLink;
    private String payId;
    private Timestamp payLinkExpireAt;
    private String planPayDate;
    private Timestamp dueDate;
    private Timestamp payFinishTime;
    private Boolean prePayment;
    private String failureReason;
    private Long accountId;
    private Long tenantId;
    private Timestamp periodStart;
    private Timestamp periodEnd;
    private Integer validFlag;
    private Timestamp createTime;
    private Timestamp lastUpdateTime;
}
